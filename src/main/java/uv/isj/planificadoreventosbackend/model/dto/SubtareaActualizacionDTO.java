package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(
        name = "SubtareaActualizacionDTO",
        description = "Campos editables de una subtarea del plan logístico")
public record SubtareaActualizacionDTO(
        @Schema(description = "Nombre de la gestión", example = "Confirmar proveedor de flores", maxLength = 200,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String nombreGestion,
        @Schema(description = "Fecha objetivo", example = "2026-11-10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate fechaObjetivo,
        @Schema(description = "Horas estimadas", example = "3", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer horasEstimadas) {
}
