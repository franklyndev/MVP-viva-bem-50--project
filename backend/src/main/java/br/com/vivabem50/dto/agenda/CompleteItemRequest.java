package br.com.vivabem50.dto.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Marca ou desmarca um item da agenda como feito. source: APPOINTMENT ou ACTIVITY. */
public record CompleteItemRequest(
        @NotBlank(message = "Informe a origem do item.")
        String source,

        @NotNull(message = "Informe o item.")
        Long sourceId,

        @NotNull(message = "Informe a data.")
        LocalDate date,

        boolean done
) {
}
