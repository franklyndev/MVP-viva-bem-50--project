package br.com.vivabem50.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "Informe sua data de nascimento.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate birthDate
) {
}
