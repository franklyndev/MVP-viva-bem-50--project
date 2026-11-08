package br.com.vivabem50.exception;

/** Responde 401 no login, sem dizer se o erro foi no e-mail ou na senha. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha incorretos.");
    }
}
