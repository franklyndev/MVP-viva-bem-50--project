package br.com.vivabem50.dto;

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
