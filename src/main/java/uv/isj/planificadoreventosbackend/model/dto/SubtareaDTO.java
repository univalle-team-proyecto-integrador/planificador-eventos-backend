package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;

@Schema(name = "SubtareaDTO", description = "Datos de una subtarea del plan logístico")
public record SubtareaDTO(
        @Schema(description = "Identificador de la subtarea", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Integer idSubtarea,
        @Schema(description = "Identificador del evento", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Integer idEvento,
        @Schema(description = "Nombre de la gestión", example = "Confirmar proveedor de flores", maxLength = 200,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String nombreGestion,
        @Schema(description = "Fecha objetivo", example = "2026-11-10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate fechaObjetivo,
        @Schema(description = "Horas estimadas", example = "3", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer horasEstimadas,
        @Schema(description = "Estado actual", example = "pendiente", accessMode = Schema.AccessMode.READ_ONLY)
        EstadoSubtarea estado,
        @Schema(description = "Nota explicativa", example = "Proveedor sin disponibilidad")
        String notaExplicativa,
        @Schema(description = "Fecha de creación", example = "2026-09-24T10:15:30",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime fechaCreacion) {
}
