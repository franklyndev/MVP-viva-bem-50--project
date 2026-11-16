package br.com.vivabem50.dto.profile;

import br.com.vivabem50.model.enums.ActivityLevel;
import jakarta.validation.constraints.Size;

/** Os dois campos são opcionais. */
public record HealthProfileRequest(
        ActivityLevel activityLevel,

        @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
        String notes
) {
}
