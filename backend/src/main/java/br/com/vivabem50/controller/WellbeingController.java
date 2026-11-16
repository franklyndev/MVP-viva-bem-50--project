package br.com.vivabem50.controller;

import br.com.vivabem50.config.AuthenticatedUser;
import br.com.vivabem50.dto.wellbeing.WellbeingResponse;
import br.com.vivabem50.dto.wellbeing.WellbeingUpdateRequest;
import br.com.vivabem50.service.UserMapper;
import br.com.vivabem50.service.WellbeingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/wellbeing")
@Tag(name = "Bem-estar")
public class WellbeingController {

    private final WellbeingService wellbeingService;
    private final UserMapper userMapper;

    public WellbeingController(WellbeingService wellbeingService, UserMapper userMapper) {
        this.wellbeingService = wellbeingService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "Água, minutos de atividade e humor de um dia (padrão: hoje)")
    public WellbeingResponse get(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return wellbeingService.get(AuthenticatedUser.id(jwt), date != null ? date : userMapper.today());
    }

    @PutMapping
    @Operation(summary = "Atualiza o registro do dia; só os campos enviados mudam")
    public WellbeingResponse update(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody WellbeingUpdateRequest request) {
        return wellbeingService.update(AuthenticatedUser.id(jwt), request);
    }
}
