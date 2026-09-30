package uv.isj.planificadoreventosbackend.exception;

/**
 * Se lanza cuando una operación exige identidad y la petición no la trae.
 * Se traduce a 401 con el mismo cuerpo ProblemDetail que el entry point de
 * Spring Security, para que el cliente reciba un único formato de error.
 */
public class SinAutenticacionException extends RuntimeException {

    public SinAutenticacionException(String message) {
        super(message);
    }
}
