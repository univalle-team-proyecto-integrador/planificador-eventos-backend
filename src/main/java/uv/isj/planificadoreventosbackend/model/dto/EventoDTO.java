package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "EventoDTO", description = "Datos de un evento del planificador")
public record EventoDTO(
        @Schema(description = "Identificador del evento", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Integer idEvento,
        @Schema(description = "Identificador del organizador", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer idUsuario,
        @Schema(description = "Identificador del tipo de evento", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer idTipoEvento,
        @Schema(description = "Nombre del evento", example = "Boda de María y Luis", maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 150) String nombre,
        @Schema(description = "Cliente del evento", example = "María", maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 150) String cliente,
        @Schema(description = "Fecha y hora del evento", example = "2026-12-01T15:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDateTime fechaEvento,
        @Schema(description = "Lugar del evento", example = "Salón El Tesoro", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 255) String lugar,
        @Schema(description = "Fecha de creación", example = "2026-09-24T10:15:30",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime fechaCreacion,
        @Schema(description = "Subtareas asociadas al evento", accessMode = Schema.AccessMode.READ_ONLY)
        List<SubtareaDTO> subtareas) {
}
