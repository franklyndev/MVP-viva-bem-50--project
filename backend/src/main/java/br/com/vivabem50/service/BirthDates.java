package br.com.vivabem50.service;

import br.com.vivabem50.exception.BusinessRuleException;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/** Regras de data de nascimento e cálculo de idade no fuso de Sergipe. */
final class BirthDates {

    static final ZoneId ZONE = ZoneId.of("America/Maceio");
    private static final LocalDate MIN = LocalDate.of(1900, 1, 1);

    private BirthDates() {
    }

    static void validate(LocalDate birthDate) {
        if (birthDate.isBefore(MIN)) {
            throw new BusinessRuleException("Informe uma data de nascimento a partir de 1900.");
        }
    }

    static int age(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now(ZONE)).getYears();
    }
}
