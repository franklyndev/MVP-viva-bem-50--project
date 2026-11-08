package br.com.vivabem50.dto.appointment;

import br.com.vivabem50.model.Appointment;
import br.com.vivabem50.model.enums.AppointmentStatus;
import br.com.vivabem50.model.enums.AppointmentType;

import java.time.OffsetDateTime;

/** startsAt volta em UTC ("2026-11-20T12:00:00Z"); o front converte para o horário local. */
public record AppointmentResponse(
        Long id,
        AppointmentType type,
        String title,
        String location,
        String notes,
        OffsetDateTime startsAt,
        Integer durationMinutes,
        AppointmentStatus status
) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getType(),
                appointment.getTitle(),
                appointment.getLocation(),
                appointment.getNotes(),
                appointment.getStartsAt(),
                appointment.getDurationMinutes(),
                appointment.getStatus());
    }
}
