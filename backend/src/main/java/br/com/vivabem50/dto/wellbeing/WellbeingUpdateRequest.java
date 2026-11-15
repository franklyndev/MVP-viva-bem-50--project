package br.com.vivabem50.dto.wellbeing;

import br.com.vivabem50.model.enums.Mood;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Só os campos enviados são alterados. */
public record WellbeingUpdateRequest(
        @NotNull(message = "Informe a data.")
        LocalDate date,

        @Min(value = 0, message = "Os copos de água não podem ser negativos.")
        @Max(value = 30, message = "Informe no máximo 30 copos de água.")
        Integer waterCups,

        @Min(value = 0, message = "Os minutos de atividade não podem ser negativos.")
        @Max(value = 600, message = "Informe no máximo 600 minutos de atividade.")
        Integer activityMinutes,

        Mood mood
) {
}
