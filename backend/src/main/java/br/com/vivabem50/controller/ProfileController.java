package br.com.vivabem50.controller;

import br.com.vivabem50.config.AuthenticatedUser;
import br.com.vivabem50.dto.profile.HealthProfileRequest;
import br.com.vivabem50.dto.profile.ProfileResponse;
import br.com.vivabem50.dto.profile.UpdateGoalsRequest;
import br.com.vivabem50.dto.profile.UpdateProfileRequest;
import br.com.vivabem50.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Perfil")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Dados do usuário logado, com idade, objetivos e perfil de saúde")
    public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return profileService.get(AuthenticatedUser.id(jwt));
    }

    @PutMapping
    @Operation(summary = "Atualiza nome e data de nascimento")
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(AuthenticatedUser.id(jwt), request);
    }

    @PutMapping("/health-profile")
    @Operation(summary = "Cria ou atualiza o perfil de saúde")
    public ProfileResponse updateHealthProfile(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody HealthProfileRequest request) {
        return profileService.updateHealthProfile(AuthenticatedUser.id(jwt), request);
    }

    @PutMapping("/goals")
    @Operation(summary = "Substitui os objetivos do usuário pelos códigos informados")
    public ProfileResponse updateGoals(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateGoalsRequest request) {
        return profileService.updateGoals(AuthenticatedUser.id(jwt), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a conta e todos os dados do usuário")
    public void delete(@AuthenticationPrincipal Jwt jwt) {
        profileService.delete(AuthenticatedUser.id(jwt));
    }
}
