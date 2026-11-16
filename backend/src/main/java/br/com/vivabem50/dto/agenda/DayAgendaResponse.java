package br.com.vivabem50.dto.agenda;

import br.com.vivabem50.dto.profile.UserResponse;
import br.com.vivabem50.dto.wellbeing.WellbeingResponse;

import java.time.LocalDate;
import java.util.List;

/** Linha do tempo de um dia: compromissos e atividades, em ordem de horário. nextAppointment pode ser null. */
public record DayAgendaResponse(
        LocalDate date,
        UserResponse user,
        TimelineItemResponse nextAppointment,
        WellbeingResponse wellbeing,
        List<TimelineItemResponse> items
) {
}
