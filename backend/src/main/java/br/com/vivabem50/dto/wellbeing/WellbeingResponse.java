package br.com.vivabem50.dto.wellbeing;

import br.com.vivabem50.model.enums.Mood;

import java.time.LocalDate;

/** Registro de bem-estar do dia. Sem registro, os valores vêm zerados e mood null. */
public record WellbeingResponse(
        LocalDate date,
        int waterCups,
        int waterGoal,
        int activityMinutes,
        int activityGoal,
        Mood mood
) {
}
