package br.com.vivabem50.dto.wellbeing;

import br.com.vivabem50.model.enums.Mood;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Só os campos enviados são alterados. */
public record WellbeingUpdateRequest(
        @NotNull(message = "Informe a data.")
        LocalDate date,

        @DecimalMin(value = "0.0", message = "Os litros de água não podem ser negativos.")
        @DecimalMax(value = "20.0", message = "Informe no máximo 20 litros de água.")
        @Digits(integer = 2, fraction = 2, message = "Informe até duas casas decimais para os litros.")
        BigDecimal waterLiters,

        // Mantido para compatibilidade com clientes anteriores à contagem em litros.
        @Min(value = 0, message = "Os copos de água não podem ser negativos.")
        @Max(value = 80, message = "Informe no máximo 80 copos de água.")
        Integer waterCups,

        @Min(value = 0, message = "Os minutos de atividade não podem ser negativos.")
        @Max(value = 600, message = "Informe no máximo 600 minutos de atividade.")
        Integer activityMinutes,

        @Min(value = 1, message = "A meta deve ter ao menos 1 minuto.")
        @Max(value = 600, message = "A meta não pode ultrapassar 600 minutos.")
        Integer activityGoal,

        Mood mood
) {
}
