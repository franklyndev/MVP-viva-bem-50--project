package br.com.vivabem50.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * history é opcional: as últimas falas da conversa, da mais antiga para a mais recente,
 * sem a mensagem atual. Com ele a assistente entende respostas como "sim, pode marcar".
 */
public record ChatRequest(
        @NotBlank(message = "Escreva sua mensagem.")
        @Size(max = 500, message = "A mensagem pode ter no máximo 500 caracteres.")
        String message,

        @Size(max = 20, message = "Envie no máximo as 20 últimas falas da conversa.")
        List<@Valid ChatTurn> history
) {
    public List<ChatTurn> historyOrEmpty() {
        return history == null ? List.of() : history;
    }
}
