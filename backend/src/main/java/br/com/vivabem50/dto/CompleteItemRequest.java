package br.com.vivabem50.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CompleteItemRequest(
        @NotBlank String source,
        @NotNull Long sourceId,
        @NotNull LocalDate date,
        boolean done
) {
}
