package uv.isj.planificadoreventosbackend.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException() {
        super("Recurso no encontrado");
    }

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}