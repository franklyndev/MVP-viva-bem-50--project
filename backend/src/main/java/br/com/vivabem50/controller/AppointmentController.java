package br.com.vivabem50.controller;

import br.com.vivabem50.config.AuthenticatedUser;
import br.com.vivabem50.dto.appointment.AppointmentRequest;
import br.com.vivabem50.dto.appointment.AppointmentResponse;
import br.com.vivabem50.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Compromissos")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "Lista os compromissos em ordem de data; filtre com from e to (ISO 8601, com fuso)")
    public List<AppointmentResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return appointmentService.list(AuthenticatedUser.id(jwt), from, to);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um compromisso")
    public AppointmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return appointmentService.get(AuthenticatedUser.id(jwt), id);
    }

    @PostMapping
    @Operation(summary = "Cria um compromisso com status AGENDADO")
    public ResponseEntity<AppointmentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse created = appointmentService.create(AuthenticatedUser.id(jwt), request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                        .buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um compromisso, inclusive data e horário")
    public AppointmentResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.update(AuthenticatedUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um compromisso")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        appointmentService.delete(AuthenticatedUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
