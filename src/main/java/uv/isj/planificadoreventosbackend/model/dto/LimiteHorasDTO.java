package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Ajuste del límite de horas diarias.
 *
 * <p>Los mensajes de {@code @Min} y {@code @Max} van explícitos y en español a
 * propósito: sin ellos Bean Validation devuelve el texto por defecto en inglés
 * ("must be less than or equal to 16"), que no le dice al cliente qué corregir.
 */
@Schema(name = "LimiteHorasDTO", description = "Límite de horas diarias del organizador")
public record LimiteHorasDTO(
        @Schema(description = "Límite de horas diarias, entre 1 y 16", example = "8",
                minimum = "1", maximum = "16", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El límite de horas diarias es obligatorio")
        @Min(value = 1, message = "El límite de horas diarias debe ser al menos 1 hora")
        @Max(value = 16, message = "El límite de horas diarias no puede superar las 16 horas")
        Integer limiteHorasDiarias) {
}