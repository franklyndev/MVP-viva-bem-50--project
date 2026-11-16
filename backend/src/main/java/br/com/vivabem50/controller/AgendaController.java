package br.com.vivabem50.controller;

import br.com.vivabem50.config.AuthenticatedUser;
import br.com.vivabem50.dto.agenda.CompleteItemRequest;
import br.com.vivabem50.dto.agenda.DayAgendaResponse;
import br.com.vivabem50.service.AgendaService;
import br.com.vivabem50.service.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/agenda")
@Tag(name = "Agenda do dia")
public class AgendaController {

    private final AgendaService agendaService;
    private final UserMapper userMapper;

    public AgendaController(AgendaService agendaService, UserMapper userMapper) {
        this.agendaService = agendaService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "Linha do tempo de um dia (padrão: hoje), com próximo compromisso e bem-estar")
    public DayAgendaResponse day(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return agendaService.day(AuthenticatedUser.id(jwt), date != null ? date : userMapper.today());
    }

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca ou desmarca um compromisso ou atividade como feito")
    public void complete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CompleteItemRequest request) {
        agendaService.complete(AuthenticatedUser.id(jwt), request);
    }
}
