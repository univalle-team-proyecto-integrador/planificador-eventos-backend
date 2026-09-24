package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(name = "ReprogramarDTO", description = "Nueva fecha y asignación de horas de una subtarea")
public record ReprogramarDTO(
        @Schema(description = "Nueva fecha objetivo", example = "2026-11-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate nuevaFecha,
        @Schema(description = "Nuevas horas estimadas", example = "3", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer nuevasHoras) {
}
