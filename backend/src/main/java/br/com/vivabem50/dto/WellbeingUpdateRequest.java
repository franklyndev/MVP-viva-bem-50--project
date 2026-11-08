package br.com.vivabem50.dto;

import br.com.vivabem50.model.enums.Mood;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WellbeingUpdateRequest(
        @NotNull LocalDate date,
        @Min(0) @Max(30) Integer waterCups,
        @Min(0) @Max(600) Integer activityMinutes,
        Mood mood
) {
}
