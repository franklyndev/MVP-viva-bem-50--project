package br.com.vivabem50.controller;

import br.com.vivabem50.dto.AppointmentResponse;
import br.com.vivabem50.dto.CompleteItemRequest;
import br.com.vivabem50.dto.CreateAppointmentRequest;
import br.com.vivabem50.dto.DayAgendaResponse;
import br.com.vivabem50.security.CurrentUser;
import br.com.vivabem50.service.AgendaService;
import br.com.vivabem50.service.UserMapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AgendaController {

    private final AgendaService agendaService;
    private final CurrentUser currentUser;
    private final UserMapper userMapper;

    public AgendaController(AgendaService agendaService, CurrentUser currentUser, UserMapper userMapper) {
        this.agendaService = agendaService;
        this.currentUser = currentUser;
        this.userMapper = userMapper;
    }

    @GetMapping("/agenda")
    public DayAgendaResponse agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate day = date != null ? date : LocalDate.now(userMapper.zoneId());
        return agendaService.day(currentUser.require(), day);
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate today = LocalDate.now(userMapper.zoneId());
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : today.plusDays(7);
        return agendaService.listAppointments(currentUser.require(), start, end);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return agendaService.createAppointment(currentUser.require(), request);
    }

    @PostMapping("/agenda/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@Valid @RequestBody CompleteItemRequest request) {
        agendaService.complete(currentUser.require(), request);
    }
}
