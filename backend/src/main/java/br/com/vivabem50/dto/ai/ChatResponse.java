package br.com.vivabem50.dto.ai;

/** agendaChanged é true quando a assistente criou, concluiu ou excluiu algo, para o app recarregar a agenda. */
public record ChatResponse(String reply, boolean agendaChanged) {
}
