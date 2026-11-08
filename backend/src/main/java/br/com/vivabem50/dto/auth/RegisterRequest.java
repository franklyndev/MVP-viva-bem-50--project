package br.com.vivabem50.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Informe uma senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String password,

        @NotNull(message = "Informe sua data de nascimento.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate birthDate,

        @NotNull(message = "É preciso aceitar o uso dos dados para criar a conta.")
        @AssertTrue(message = "É preciso aceitar o uso dos dados para criar a conta.")
        Boolean dataConsent
) {
}
