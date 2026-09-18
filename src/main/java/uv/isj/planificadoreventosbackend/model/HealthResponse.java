package uv.isj.planificadoreventosbackend.model;

import java.time.OffsetDateTime;

public record HealthResponse(String status, String database, OffsetDateTime timestamp) {
}