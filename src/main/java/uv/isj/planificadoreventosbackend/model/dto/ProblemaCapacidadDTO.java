package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Cuerpo del 409 que devuelve {@code PATCH /api/subtareas/{id}/reprogramar} cuando
 * la reprogramación supera el límite diario. La subtarea no se guarda.
 *
 * <p>Es la forma que produce {@code GlobalExceptionHandler}: un {@code ProblemDetail}
 * cuyas propiedades quedan <strong>aplanadas en el nivel raíz</strong>, no anidadas
 * bajo una clave {@code properties}. Spring las serializa así y por eso este schema
 * las declara todas al mismo nivel.
 *
 * <p>Existe para documentar el 409 con su forma real. Antes springdoc deducía
 * {@code SubtareaDTO} —el tipo de retorno del método— y la documentación mentía.
 */
@Schema(name = "ProblemaCapacidadDTO",
        description = "Conflicto de límite diario (409). La subtarea no se modificó")
public record ProblemaCapacidadDTO(
        @Schema(description = "Código de la respuesta", example = "409",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer status,

        @Schema(description = "Resumen del conflicto", example = "Límite diario excedido",
                accessMode = Schema.AccessMode.READ_ONLY)
        String title,

        @Schema(description = "Detalle legible del conflicto",
                example = "La reprogramación supera el límite diario de 5 horas",
                accessMode = Schema.AccessMode.READ_ONLY)
        String detail,

        @Schema(description = "Límite diario configurado", example = "5",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer limiteDiario,

        @Schema(description = "Horas ya asignadas ese día, sin contar esta subtarea", example = "3",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer horasAsignadasPreviamente,

        @Schema(description = "Horas pedidas en la reprogramación", example = "3",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer horasSolicitadas,

        @Schema(description = "Suma que supera el límite", example = "6",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer horasPlanificadasTotales,

        @Schema(description = "Cuántas horas hay que liberar", example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer excedente,

        @Schema(description = "Fecha que se quiso asignar", example = "2026-11-20",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDate fecha,

        @Schema(description = "Subtarea que no se movió", example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer idSubtarea) {
}
