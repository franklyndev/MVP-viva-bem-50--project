package br.com.vivabem50.dto.wellbeing;

import br.com.vivabem50.model.enums.Mood;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de bem-estar do dia. Sem registro, os valores vêm zerados e mood null. */
public record WellbeingResponse(
        LocalDate date,
        BigDecimal waterLiters,
        BigDecimal waterGoalLiters,
        int activityMinutes,
        int activityGoal,
        Mood mood
) {
    /** Compatibilidade para clientes antigos; novos clientes devem usar litros. */
    @JsonProperty("waterCups")
    public int waterCups() {
        return waterLiters.multiply(BigDecimal.valueOf(4)).intValue();
    }

    @JsonProperty("waterGoal")
    public int waterGoal() {
        return waterGoalLiters.multiply(BigDecimal.valueOf(4)).intValue();
    }
}
