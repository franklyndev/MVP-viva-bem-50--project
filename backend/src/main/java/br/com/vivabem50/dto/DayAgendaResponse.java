package br.com.vivabem50.dto;

import java.time.LocalDate;
import java.util.List;

public record DayAgendaResponse(
        LocalDate date,
        UserResponse user,
        TimelineItemResponse nextAppointment,
        WellbeingResponse wellbeing,
        List<TimelineItemResponse> items
) {
}
