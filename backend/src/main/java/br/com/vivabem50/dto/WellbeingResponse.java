package br.com.vivabem50.dto;

import br.com.vivabem50.model.enums.Mood;

import java.time.LocalDate;

public record WellbeingResponse(
        LocalDate date,
        int waterCups,
        int waterGoal,
        int activityMinutes,
        int activityGoal,
        Mood mood
) {
}
