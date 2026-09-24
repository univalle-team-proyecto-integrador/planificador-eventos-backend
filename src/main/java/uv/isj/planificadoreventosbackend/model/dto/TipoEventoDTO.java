package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TipoEventoDTO", description = "Tipo de evento disponible para el organizador")
public record TipoEventoDTO(
        @Schema(description = "Identificador del tipo de evento", example = "1")
        Integer idTipoEvento,
        @Schema(description = "Nombre del tipo de evento", example = "Boda")
        @NotBlank @Size(max = 50) String nombre) {
}
