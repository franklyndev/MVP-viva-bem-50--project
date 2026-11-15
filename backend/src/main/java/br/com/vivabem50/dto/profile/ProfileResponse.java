package br.com.vivabem50.dto.profile;

import java.time.LocalDate;
import java.util.List;

/** city e healthProfile podem ser null enquanto o usuário não preencher esses dados. */
public record ProfileResponse(
        Long id,
        String name,
        String email,
        LocalDate birthDate,
        int age,
        String city,
        List<GoalResponse> goals,
        HealthProfileResponse healthProfile
) {
}
