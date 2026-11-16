package br.com.vivabem50.exception;

/** Responde 400 quando os dados têm formato válido, mas quebram uma regra de negócio. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
