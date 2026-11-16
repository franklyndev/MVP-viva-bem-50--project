package br.com.vivabem50.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Uma fala anterior da conversa. role: "user" (a pessoa) ou "assistant" (a Vivi). */
public record ChatTurn(
        @NotBlank(message = "Informe quem falou.")
        @Pattern(regexp = "user|assistant", message = "Use 'user' ou 'assistant'.")
        String role,

        @NotBlank(message = "A fala não pode ficar vazia.")
        @Size(max = 2000, message = "Cada fala do histórico pode ter no máximo 2000 caracteres.")
        String content
) {
}
