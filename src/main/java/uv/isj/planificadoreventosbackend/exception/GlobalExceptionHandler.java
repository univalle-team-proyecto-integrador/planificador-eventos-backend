package uv.isj.planificadoreventosbackend.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleRecursoNoEncontrado(
            RecursoNoEncontradoException ex) {
        ProblemDetail problem = crearProblema(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleArgumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail problem = crearProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                ex.getMessage());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleCuerpoInvalido(
            HttpMessageNotReadableException ex) {
        ProblemDetail problem = crearProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El cuerpo de la solicitud no contiene un JSON válido");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = crearProblema(
                HttpStatus.BAD_REQUEST,
                "Datos inválidos",
                "Uno o más campos enviados no son válidos");
        problem.setProperty("errors", errores);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleParametroAusente(
            MissingServletRequestParameterException ex) {
        ProblemDetail problem = crearProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'");
        problem.setProperty("errors", Map.of(ex.getParameterName(), "parámetro obligatorio"));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTipoInvalido(
            MethodArgumentTypeMismatchException ex) {
        String valor = ex.getValue() == null ? "vacío" : "'" + ex.getValue() + "'";
        ProblemDetail problem = crearProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El valor " + valor + " no es válido para el parámetro '" + ex.getName() + "'");
        problem.setProperty("errors", Map.of(ex.getName(), "tipo o formato incorrecto"));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex) {
        List<String> permitidos = ex.getSupportedHttpMethods() == null
                ? List.of()
                : ex.getSupportedHttpMethods().stream().map(HttpMethod::name).toList();

        ProblemDetail problem = crearProblema(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método no permitido",
                "El método " + ex.getMethod() + " no está soportado en esta ruta"
                        + (permitidos.isEmpty() ? "" : ". Permitidos: " + String.join(", ", permitidos)));

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (!permitidos.isEmpty()) {
            respuesta.header(HttpHeaders.ALLOW, String.join(", ", permitidos));
        }
        return respuesta.body(problem);
    }

    private ProblemDetail crearProblema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        return problem;
    }
}
