package br.com.vivabem50.service;

import br.com.vivabem50.exception.BusinessRuleException;

import java.time.LocalDate;

/** Regra de data de nascimento. A idade é calculada pelo UserMapper, no fuso configurado. */
final class BirthDates {

    private static final LocalDate MIN = LocalDate.of(1900, 1, 1);

    private BirthDates() {
    }

    static void validate(LocalDate birthDate) {
        if (birthDate.isBefore(MIN)) {
            throw new BusinessRuleException("Informe uma data de nascimento a partir de 1900.");
        }
    }
}
