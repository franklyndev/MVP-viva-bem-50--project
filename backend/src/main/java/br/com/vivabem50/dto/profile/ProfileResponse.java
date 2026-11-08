package br.com.vivabem50.dto.profile;

import java.time.LocalDate;
import java.util.List;

/** healthProfile é null enquanto o usuário não preencher o perfil de saúde. */
public record ProfileResponse(
        Long id,
        String name,
        String email,
        LocalDate birthDate,
        int age,
        List<GoalResponse> goals,
        HealthProfileResponse healthProfile
) {
}
