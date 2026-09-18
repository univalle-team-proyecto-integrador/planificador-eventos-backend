package uv.isj.planificadoreventosbackend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;

public record SubtareaDTO(
        Integer idSubtarea,
        @NotNull Integer idEvento,
        @NotBlank @Size(max = 200) String nombreGestion,
        @NotNull LocalDate fechaObjetivo,
        @NotNull @Min(1) Integer horasEstimadas,
        EstadoSubtarea estado,
        String notaExplicativa,
        LocalDateTime fechaCreacion) {
}