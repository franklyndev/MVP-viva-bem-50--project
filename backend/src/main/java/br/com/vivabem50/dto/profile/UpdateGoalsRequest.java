package br.com.vivabem50.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Substitui todos os objetivos do usuário. Lista vazia remove todos. */
public record UpdateGoalsRequest(
        @NotNull(message = "Informe a lista de objetivos.")
        @Size(max = 10, message = "Escolha no máximo 10 objetivos.")
        List<@NotBlank(message = "O código do objetivo não pode ficar vazio.") String> goalCodes
) {
}
