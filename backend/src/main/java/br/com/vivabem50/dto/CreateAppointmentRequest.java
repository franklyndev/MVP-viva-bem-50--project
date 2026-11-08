package br.com.vivabem50.dto;

import br.com.vivabem50.model.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
        @NotNull AppointmentType type,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 160) String location,
        String notes,
        @NotNull OffsetDateTime startsAt,
        @Positive Integer durationMinutes
) {
}
