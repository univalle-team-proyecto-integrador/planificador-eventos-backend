package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;

@Schema(name = "EstadoSubtareaDTO",
        description = "Cambio de estado de una subtarea. El estado es obligatorio y la nota "
                + "explicativa es opcional; se guarda tal cual se envíe.")
public record EstadoSubtareaDTO(
        @Schema(description = "Nuevo estado de la gestión. Valores admitidos: "
                + "pendiente (por hacer), ejecutada (ya hecha) o pospuesta (aplazada). "
                + "Volver a pendiente reabre la gestión.",
                example = "pospuesta",
                allowableValues = {"pendiente", "ejecutada", "pospuesta"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull EstadoSubtarea estado,
        @Schema(description = "Motivo o nota explicativa del cambio. Opcional; útil al "
                + "pospuesta o ejecutada para dejar constancia. Puede ser null.",
                example = "Proveedor sin disponibilidad",
                nullable = true)
        String notaExplicativa) {
}
