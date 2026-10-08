package uv.isj.planificadoreventosbackend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(CapacidadExcedidaException.class)
    public ResponseEntity<ProblemDetail> handleCapacidadExcedida(CapacidadExcedidaException ex) {
        ProblemDetail problem = crearProblema(
                HttpStatus.CONFLICT,
                "Límite diario excedido",
                ex.getMessage());

        Map<String, Object> conflicto = new LinkedHashMap<>();
        conflicto.put("idSubtarea", ex.getIdSubtarea());
        conflicto.put("fecha", ex.getFecha());
        conflicto.put("limiteDiario", ex.getLimiteDiario());
        conflicto.put("horasAsignadasPreviamente", ex.getHorasAsignadasPreviamente());
        conflicto.put("horasSolicitadas", ex.getHorasSolicitadas());
        conflicto.put("horasPlanificadasTotales", ex.getHorasPlanificadasTotales());
        conflicto.put("excedente", ex.getExcedente());
        problem.setProperties(conflicto);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
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

    private ProblemDetail crearProblema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        return problem;
    }
}
