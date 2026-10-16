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
        List<SubtareaDTO> proximas,

        @Schema(description = "Subtareas pospuestas. Van aparte porque no tienen fecha efectiva: "
                + "conservan la última que tuvieron pero no comprometen ningún día ni cuentan para "
                + "el límite diario. Aparecen aquí para que sigan siendo alcanzables y se puedan "
                + "reprogramar",
                accessMode = Schema.AccessMode.READ_ONLY)
        List<SubtareaDTO> pospuestas) {

    /** Constructor de los tres grupos temporales, sin pospuestas. */
    public HoyResponseDTO(
            List<SubtareaDTO> vencidas,
            List<SubtareaDTO> paraHoy,
            List<SubtareaDTO> proximas) {
        this(vencidas, paraHoy, proximas, List.of());
    }
}
