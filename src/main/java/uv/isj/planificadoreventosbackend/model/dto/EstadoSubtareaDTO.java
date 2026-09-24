package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;

@Schema(name = "EstadoSubtareaDTO", description = "Cambio de estado de una subtarea")
public record EstadoSubtareaDTO(
        @Schema(description = "Nuevo estado; solo se admiten ejecutada o pospuesta", example = "pospuesta",
                allowableValues = {"ejecutada", "pospuesta"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull EstadoSubtarea estado,
        @Schema(description = "Motivo o nota explicativa", example = "Proveedor sin disponibilidad")
        String notaExplicativa) {
}
