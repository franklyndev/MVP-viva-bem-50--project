package br.com.vivabem50.service;

import br.com.vivabem50.dto.agenda.CompleteItemRequest;
import br.com.vivabem50.dto.agenda.DayAgendaResponse;
import br.com.vivabem50.dto.agenda.TimelineItemResponse;
import br.com.vivabem50.exception.BusinessRuleException;
import br.com.vivabem50.exception.NotFoundException;
import br.com.vivabem50.model.Activity;
import br.com.vivabem50.model.ActivityRecord;
import br.com.vivabem50.model.Appointment;
import br.com.vivabem50.model.User;
import br.com.vivabem50.model.enums.ActivityRecordStatus;
import br.com.vivabem50.model.enums.ActivityType;
import br.com.vivabem50.model.enums.AppointmentStatus;
import br.com.vivabem50.model.enums.AppointmentType;
import br.com.vivabem50.repository.ActivityRecordRepository;
import br.com.vivabem50.repository.ActivityRepository;
import br.com.vivabem50.repository.AppointmentRepository;
import br.com.vivabem50.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/** Agenda do dia: junta compromissos e atividades em uma linha do tempo e marca itens como feitos. */
@Service
public class AgendaService {

    public static final String APPOINTMENT = "APPOINTMENT";
    public static final String ACTIVITY = "ACTIVITY";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final WellbeingService wellbeingService;

    public AgendaService(AppointmentRepository appointmentRepository, ActivityRepository activityRepository,
                         ActivityRecordRepository activityRecordRepository, UserRepository userRepository,
                         UserMapper userMapper, WellbeingService wellbeingService) {
        this.appointmentRepository = appointmentRepository;
        this.activityRepository = activityRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.wellbeingService = wellbeingService;
    }

    @Transactional(readOnly = true)
    public DayAgendaResponse day(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        List<TimelineItemResponse> items = timeline(userId, date);
        TimelineItemResponse next = items.stream()
                .filter(item -> APPOINTMENT.equals(item.source()) && !item.done())
                .findFirst()
                .orElse(null);
        return new DayAgendaResponse(date, userMapper.toResponse(user), next,
                wellbeingService.get(userId, date), items);
    }

    @Transactional
    public void complete(Long userId, CompleteItemRequest request) {
        String source = request.source().trim().toUpperCase();
        if (APPOINTMENT.equals(source)) {
            Appointment appointment = appointmentRepository.findByIdAndUserId(request.sourceId(), userId)
                    .orElseThrow(() -> new NotFoundException("Compromisso não encontrado."));
            appointment.setStatus(request.done() ? AppointmentStatus.REALIZADO : AppointmentStatus.AGENDADO);
            return;
        }
        if (ACTIVITY.equals(source)) {
            Activity activity = activityRepository.findByIdAndUserId(request.sourceId(), userId)
                    .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));
            var existing = activityRecordRepository.findByActivityIdAndPerformedOn(activity.getId(), request.date());
            if (request.done()) {
                ActivityRecord record = existing.orElseGet(ActivityRecord::new);
                record.setActivity(activity);
                record.setPerformedOn(request.date());
                record.setStatus(ActivityRecordStatus.CONCLUIDA);
                activityRecordRepository.save(record);
            } else {
                existing.ifPresent(activityRecordRepository::delete);
            }
            return;
        }
        throw new BusinessRuleException("Origem inválida. Use APPOINTMENT ou ACTIVITY.");
    }

    /** Data e horário locais convertidos para o fuso configurado. */
    public static OffsetDateTime at(LocalDate date, LocalTime time, ZoneId zone) {
        return date.atTime(time).atZone(zone).toOffsetDateTime();
    }

    private List<TimelineItemResponse> timeline(Long userId, LocalDate date) {
        ZoneId zone = userMapper.zoneId();
        OffsetDateTime start = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        short isoDay = (short) date.getDayOfWeek().getValue();

        List<Appointment> dayAppointments = appointmentRepository
                .findByUserIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(userId, start, end);
        List<Activity> dayActivities = activityRepository
                .findByUserIdAndDayOfWeekAndActiveTrueOrderByTimeOfDayAsc(userId, isoDay);
        Map<Long, ActivityRecord> recordsByActivity = dayActivities.isEmpty()
                ? Map.of()
                : activityRecordRepository
                .findByActivityIdInAndPerformedOn(dayActivities.stream().map(Activity::getId).toList(), date)
                .stream()
                .collect(Collectors.toMap(record -> record.getActivity().getId(), Function.identity()));

        List<TimelineItemResponse> items = new ArrayList<>();
        for (Appointment appointment : dayAppointments) {
            boolean done = appointment.getStatus() == AppointmentStatus.REALIZADO
                    || appointment.getStatus() == AppointmentStatus.CANCELADO;
            items.add(new TimelineItemResponse(
                    APPOINTMENT + ":" + appointment.getId(),
                    APPOINTMENT,
                    appointment.getId(),
                    TIME.format(appointment.getStartsAt().atZoneSameInstant(zone)),
                    appointment.getTitle(),
                    kindOf(appointment.getType()),
                    detailOf(appointment),
                    done));
        }
        for (Activity activity : dayActivities) {
            ActivityRecord record = recordsByActivity.get(activity.getId());
            boolean done = record != null && record.getStatus() == ActivityRecordStatus.CONCLUIDA;
            items.add(new TimelineItemResponse(
                    ACTIVITY + ":" + activity.getId(),
                    ACTIVITY,
                    activity.getId(),
                    TIME.format(activity.getTimeOfDay()),
                    activity.getTitle(),
                    "Atividade",
                    detailOf(activity),
                    done));
        }
        items.sort(Comparator.comparing(TimelineItemResponse::time).thenComparing(TimelineItemResponse::id));
        return items;
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
        String type = labelOf(activity.getType());
        return activity.getDurationMinutes() == null
                ? type
                : type + ", " + activity.getDurationMinutes() + " minutos";
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
}
