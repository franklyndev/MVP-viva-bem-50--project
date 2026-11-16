package br.com.vivabem50.service;

import br.com.vivabem50.dto.appointment.AppointmentRequest;
import br.com.vivabem50.dto.appointment.AppointmentResponse;
import br.com.vivabem50.exception.BusinessRuleException;
import br.com.vivabem50.exception.NotFoundException;
import br.com.vivabem50.model.Appointment;
import br.com.vivabem50.repository.AppointmentRepository;
import br.com.vivabem50.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    /** Sem filtro, lista todos. Com filtro, os dois limites são obrigatórios e o intervalo é [from, to). */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(Long userId, OffsetDateTime from, OffsetDateTime to) {
        List<Appointment> appointments;
        if (from == null && to == null) {
            appointments = appointmentRepository.findByUserIdOrderByStartsAtAsc(userId);
        } else if (from == null || to == null) {
            throw new BusinessRuleException("Informe 'from' e 'to' juntos para filtrar por período.");
        } else if (!from.isBefore(to)) {
            throw new BusinessRuleException("'from' deve ser anterior a 'to'.");
        } else {
            appointments = appointmentRepository
                    .findByUserIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(userId, from, to);
        }
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long userId, Long id) {
        return AppointmentResponse.from(findOwned(userId, id));
    }

    @Transactional
    public AppointmentResponse create(Long userId, AppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado.")));
        apply(appointment, request);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(Long userId, Long id, AppointmentRequest request) {
        Appointment appointment = findOwned(userId, id);
        apply(appointment, request);
        return AppointmentResponse.from(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        appointmentRepository.delete(findOwned(userId, id));
    }

    private Appointment findOwned(Long userId, Long id) {
        return appointmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Compromisso não encontrado."));
    }

    private static void apply(Appointment appointment, AppointmentRequest request) {
        appointment.setType(request.type());
        appointment.setTitle(request.title().trim());
        appointment.setLocation(blankToNull(request.location()));
        appointment.setNotes(blankToNull(request.notes()));
        appointment.setStartsAt(request.startsAt());
        appointment.setDurationMinutes(request.durationMinutes());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
