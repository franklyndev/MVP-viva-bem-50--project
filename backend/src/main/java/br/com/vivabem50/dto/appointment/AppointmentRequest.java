package br.com.vivabem50.dto.appointment;

import br.com.vivabem50.model.enums.AppointmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** Usado para criar e para editar. startsAt deve ter fuso: "2026-11-20T09:00:00-03:00". */
public record AppointmentRequest(
        @NotNull(message = "Informe o tipo do compromisso.")
        AppointmentType type,

        @NotBlank(message = "Informe o título.")
        @Size(max = 120, message = "O título pode ter no máximo 120 caracteres.")
        String title,

        @Size(max = 160, message = "O local pode ter no máximo 160 caracteres.")
        String location,

        @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
        String notes,

        @NotNull(message = "Informe a data e o horário.")
        OffsetDateTime startsAt,

        @Positive(message = "A duração deve ser maior que zero.")
        @Max(value = 1440, message = "A duração pode ser de no máximo 24 horas.")
        Integer durationMinutes
) {
}
