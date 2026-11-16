package br.com.vivabem50.dto.agenda;

/**
 * Item da linha do tempo. source é APPOINTMENT ou ACTIVITY, e sourceId é o id do registro de origem.
 * time vem no formato HH:mm, já no horário local.
 */
public record TimelineItemResponse(
        String id,
        String source,
        Long sourceId,
        String time,
        String title,
        String kind,
        String detail,
        boolean done
) {
}
