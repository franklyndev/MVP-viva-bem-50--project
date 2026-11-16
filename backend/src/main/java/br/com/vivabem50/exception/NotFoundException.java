package br.com.vivabem50.exception;

/** Responde 404. Também é usada quando o registro existe, mas pertence a outro usuário. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
