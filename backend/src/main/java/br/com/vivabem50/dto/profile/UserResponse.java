package br.com.vivabem50.dto.profile;

import java.time.LocalDate;

/** Dados do usuário devolvidos no login, no cadastro e na agenda do dia. city pode ser null. */
public record UserResponse(Long id, String name, String email, LocalDate birthDate, int age, String city) {
}
