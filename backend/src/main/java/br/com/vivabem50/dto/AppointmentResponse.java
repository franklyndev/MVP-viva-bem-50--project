package br.com.vivabem50.dto;

import br.com.vivabem50.model.enums.AppointmentStatus;
import br.com.vivabem50.model.enums.AppointmentType;

import java.time.OffsetDateTime;

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
}
