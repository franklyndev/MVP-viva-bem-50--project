package br.com.vivabem50.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDate birthDate,
        int age,
        String city
) {
}
