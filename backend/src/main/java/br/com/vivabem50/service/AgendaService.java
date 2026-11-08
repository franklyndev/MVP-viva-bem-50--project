package br.com.vivabem50.service;

import br.com.vivabem50.dto.AppointmentResponse;
import br.com.vivabem50.dto.CompleteItemRequest;
import br.com.vivabem50.dto.CreateAppointmentRequest;
import br.com.vivabem50.dto.DayAgendaResponse;
import br.com.vivabem50.dto.TimelineItemResponse;
import br.com.vivabem50.model.Activity;
import br.com.vivabem50.model.ActivityRecord;
import br.com.vivabem50.model.Appointment;
import br.com.vivabem50.model.Reminder;
import br.com.vivabem50.model.User;
import br.com.vivabem50.model.enums.ActivityRecordStatus;
import br.com.vivabem50.model.enums.ActivityType;
import br.com.vivabem50.model.enums.AppointmentStatus;
import br.com.vivabem50.model.enums.AppointmentType;
import br.com.vivabem50.repository.ActivityRecordRepository;
import br.com.vivabem50.repository.ActivityRepository;
import br.com.vivabem50.repository.AppointmentRepository;
import br.com.vivabem50.repository.ReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgendaService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointments;
    private final ActivityRepository activities;
    private final ActivityRecordRepository records;
    private final ReminderRepository reminders;
    private final UserMapper userMapper;
    private final WellbeingService wellbeingService;

    public AgendaService(
            AppointmentRepository appointments,
            ActivityRepository activities,
            ActivityRecordRepository records,
            ReminderRepository reminders,
            UserMapper userMapper,
            WellbeingService wellbeingService
    ) {
        this.appointments = appointments;
        this.activities = activities;
        this.records = records;
        this.reminders = reminders;
        this.userMapper = userMapper;
        this.wellbeingService = wellbeingService;
    }

    @Transactional(readOnly = true)
    public DayAgendaResponse day(User user, LocalDate date) {
        List<TimelineItemResponse> items = timeline(user, date);
        TimelineItemResponse next = items.stream()
                .filter(item -> "APPOINTMENT".equals(item.source()) && !item.done())
                .findFirst()
                .orElse(null);
        return new DayAgendaResponse(date, userMapper.toResponse(user), next, wellbeingService.get(user, date), items);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(User user, LocalDate from, LocalDate to) {
        ZoneId zone = userMapper.zoneId();
        OffsetDateTime start = from.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return appointments
                .findByUser_IdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(user.getId(), start, end)
                .stream()
                .map(this::toAppointment)
                .toList();
    }

    @Transactional
    public AppointmentResponse createAppointment(User user, CreateAppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setType(request.type());
        appointment.setTitle(request.title().trim());
        appointment.setLocation(blankToNull(request.location()));
        appointment.setNotes(blankToNull(request.notes()));
        appointment.setStartsAt(request.startsAt());
        appointment.setDurationMinutes(request.durationMinutes());
        appointment.setStatus(AppointmentStatus.AGENDADO);
        appointments.save(appointment);

        OffsetDateTime remindAt = request.startsAt().minusHours(1);
        if (remindAt.isAfter(OffsetDateTime.now())) {
            Reminder reminder = new Reminder();
            reminder.setAppointment(appointment);
            reminder.setRemindAt(remindAt);
            reminders.save(reminder);
        }
        return toAppointment(appointment);
    }

    @Transactional
    public void complete(User user, CompleteItemRequest request) {
        if ("APPOINTMENT".equalsIgnoreCase(request.source())) {
            Appointment appointment = appointments.findByIdAndUser_Id(request.sourceId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            appointment.setStatus(request.done() ? AppointmentStatus.REALIZADO : AppointmentStatus.AGENDADO);
            return;
        }
        if ("ACTIVITY".equalsIgnoreCase(request.source())) {
            Activity activity = activities.findByIdAndUser_Id(request.sourceId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            var existing = records.findByActivity_IdAndPerformedOn(activity.getId(), request.date());
            if (request.done()) {
                ActivityRecord record = existing.orElseGet(ActivityRecord::new);
                record.setActivity(activity);
                record.setPerformedOn(request.date());
                record.setStatus(ActivityRecordStatus.CONCLUIDA);
                records.save(record);
            } else {
                existing.ifPresent(records::delete);
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origem inválida.");
    }

    private List<TimelineItemResponse> timeline(User user, LocalDate date) {
        ZoneId zone = userMapper.zoneId();
        OffsetDateTime start = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        short isoDay = (short) date.getDayOfWeek().getValue();

        List<Appointment> dayAppointments = appointments
                .findByUser_IdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(user.getId(), start, end);
        List<Activity> dayActivities = activities
                .findByUser_IdAndDayOfWeekAndActiveTrueOrderByTimeOfDayAsc(user.getId(), isoDay);

        Map<Long, ActivityRecord> recordsByActivity = dayActivities.isEmpty()
                ? Map.of()
                : records.findByActivity_IdInAndPerformedOn(dayActivities.stream().map(Activity::getId).toList(), date)
                .stream()
                .collect(Collectors.toMap(record -> record.getActivity().getId(), Function.identity()));

        List<TimelineItemResponse> items = new ArrayList<>();
        for (Appointment appointment : dayAppointments) {
            boolean done = appointment.getStatus() == AppointmentStatus.REALIZADO
                    || appointment.getStatus() == AppointmentStatus.CANCELADO;
            items.add(new TimelineItemResponse(
                    "APPOINTMENT:" + appointment.getId(),
                    "APPOINTMENT",
                    appointment.getId(),
                    TIME.format(appointment.getStartsAt().atZoneSameInstant(zone)),
                    appointment.getTitle(),
                    kindOf(appointment.getType()),
                    detailOf(appointment),
                    done
            ));
        }
        for (Activity activity : dayActivities) {
            ActivityRecord record = recordsByActivity.get(activity.getId());
            boolean done = record != null && record.getStatus() == ActivityRecordStatus.CONCLUIDA;
            items.add(new TimelineItemResponse(
                    "ACTIVITY:" + activity.getId(),
                    "ACTIVITY",
                    activity.getId(),
                    TIME.format(activity.getTimeOfDay()),
                    activity.getTitle(),
                    "Atividade",
                    detailOf(activity),
                    done
            ));
        }
        items.sort(Comparator.comparing(TimelineItemResponse::time).thenComparing(TimelineItemResponse::id));
        return items;
    }

    private AppointmentResponse toAppointment(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getType(),
                appointment.getTitle(),
                appointment.getLocation(),
                appointment.getNotes(),
                appointment.getStartsAt(),
                appointment.getDurationMinutes(),
                appointment.getStatus()
        );
    }

    private static String kindOf(AppointmentType type) {
        return switch (type) {
            case CONSULTA -> "Consulta";
            case EXAME -> "Exame";
            case OUTRO -> "Lembrete";
        };
    }

    private static String detailOf(Appointment appointment) {
        List<String> parts = new ArrayList<>();
        if (appointment.getLocation() != null && !appointment.getLocation().isBlank()) {
            parts.add(appointment.getLocation());
        }
        if (appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
            parts.add(appointment.getNotes());
        }
        return String.join(" · ", parts);
    }

    private static String detailOf(Activity activity) {
        String duration = activity.getDurationMinutes() == null
                ? ""
                : activity.getDurationMinutes() + " minutos";
        String type = labelOf(activity.getType());
        if (duration.isBlank()) {
            return type;
        }
        return type + ", " + duration;
    }

    private static String labelOf(ActivityType type) {
        return switch (type) {
            case CAMINHADA -> "Caminhada";
            case MOBILIDADE -> "Mobilidade";
            case FORCA -> "Força";
            case EQUILIBRIO -> "Equilíbrio";
            case OUTRA -> "Atividade";
        };
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static OffsetDateTime at(LocalDate date, LocalTime time, ZoneId zone) {
        return date.atTime(time).atZone(zone).toOffsetDateTime();
    }
}
