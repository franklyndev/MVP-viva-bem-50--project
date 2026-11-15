package br.com.vivabem50.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Escreva sua mensagem.")
        @Size(max = 500, message = "A mensagem pode ter no máximo 500 caracteres.")
        String message
) {
}
