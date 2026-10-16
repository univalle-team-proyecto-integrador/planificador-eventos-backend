package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Forma común de error de la API (RFC 7807).
 *
 * <p>Existe para documentar las respuestas 400/401/404 sin mentir: springdoc
 * deduciría el tipo de retorno del método (por ejemplo {@code SubtareaDTO}) y el
 * contrato publicado no coincidiría con el cuerpo real. Los conflictos de
 * capacidad tienen su propio schema, {@link ProblemaCapacidadDTO}, porque
 * además del mensaje llevan el detalle del excedente.
 *
 * <p>Spring serializa las propiedades del {@code ProblemDetail} en el nivel
 * raíz, no anidadas bajo una clave {@code properties}; por eso este schema
 * declara los campos al mismo nivel.
 */
@Schema(name = "ProblemaDTO",
        description = "Error de la API en formato RFC 7807")
public record ProblemaDTO(
        @Schema(description = "URI que identifica el tipo de problema",
                example = "about:blank",
                accessMode = Schema.AccessMode.READ_ONLY)
        String type,

        @Schema(description = "Resumen legible del problema",
                example = "Datos inválidos",
                accessMode = Schema.AccessMode.READ_ONLY)
        String title,

        @Schema(description = "Código HTTP de la respuesta",
                example = "400",
                accessMode = Schema.AccessMode.READ_ONLY)
        Integer status,

        @Schema(description = "Detalle legible del problema",
                example = "Uno o más campos enviados no son válidos",
                accessMode = Schema.AccessMode.READ_ONLY)
        String detail,

        @Schema(description = "Errores por campo cuando falla la validación; "
                + "ausente en los errores que no son de validación",
                example = "{\"estado\":\"no debe ser nulo\"}",
                accessMode = Schema.AccessMode.READ_ONLY)
        Map<String, String> errors) {
}
