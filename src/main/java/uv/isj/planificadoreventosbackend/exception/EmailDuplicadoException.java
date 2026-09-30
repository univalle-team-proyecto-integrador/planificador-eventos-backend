package uv.isj.planificadoreventosbackend.exception;

/** El correo ya está registrado. Se traduce a 409. */
public class EmailDuplicadoException extends RuntimeException {

    public EmailDuplicadoException(String message) {
        super(message);
    }
}
