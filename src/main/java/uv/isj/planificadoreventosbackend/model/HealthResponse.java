package uv.isj.planificadoreventosbackend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "HealthResponse", description = "Estado de la aplicación y de la base de datos")
public record HealthResponse(
        @Schema(description = "Estado de la aplicación", example = "healthy",
                allowableValues = {"healthy", "unhealthy"})
        String status,
        @Schema(description = "Estado de la conexión", example = "connected",
                allowableValues = {"connected", "disconnected"})
        String database,
        @Schema(description = "Momento de la comprobación", example = "2026-09-24T10:15:30-05:00")
        OffsetDateTime timestamp) {
}
