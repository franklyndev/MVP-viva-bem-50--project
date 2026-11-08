package br.com.vivabem50.controller;

import br.com.vivabem50.dto.profile.GoalResponse;
import br.com.vivabem50.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@Tag(name = "Perfil")
@SecurityRequirements
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    @Operation(summary = "Lista os objetivos disponíveis (rota pública, usada no cadastro)")
    public List<GoalResponse> list() {
        return goalService.listAll();
    }
}
