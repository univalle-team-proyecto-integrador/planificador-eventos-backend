package uv.isj.planificadoreventosbackend.exception;

/**
 * Credenciales inválidas. Se usa tanto para "el correo no existe" como para
 * "la contraseña no coincide": el mismo mensaje evita que el login sirva como
 * oráculo para averiguar qué correos están registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
