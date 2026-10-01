package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "HoyResponseDTO", description = "Subtareas del día agrupadas por estado temporal")
public record HoyResponseDTO(
        @Schema(description = "Subtareas con fecha objetivo anterior a hoy")
        List<SubtareaDTO> vencidas,

        @Schema(description = "Subtareas con fecha objetivo igual a hoy")
        List<SubtareaDTO> paraHoy,

        @Schema(description = "Subtareas con fecha objetivo posterior a hoy")
        List<SubtareaDTO> proximas) {
}
