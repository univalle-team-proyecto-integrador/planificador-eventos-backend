---
tipo: mejora
---

# Endpoint de estado /api/health

- **Fecha:** 2026-09-18
- **Área:** API
- **Estado:** hecha

## Descripción

Se añadió un endpoint de verificación de salud para usarlo como health check en el despliegue (Render) y para diagnóstico manual. Devuelve estado de la aplicación y de la base de datos.

## Cambios

- `model/HealthResponse.java` — record con `status`, `database`, `timestamp` (OffsetDateTime).
- `service/HealthService.java` — sondeo de la BD con `JdbcTemplate` (`SELECT 1`) mediante HikariCP; si falla, estado `unhealthy`/`disconnected` (HTTP 503).
- `controller/HealthController.java` — `@GetMapping({"/api/health", "/api/health/"})`; 200 si todo está bien, 503 si la BD no responde.
- `controller/HealthControllerTest.java` — MockMvc + H2 (perfil `test`); `@AutoConfigureMockMvc` en Boot 4 vive en `org.springframework.boot.webmvc.test.autoconfigure`.
- `service/HealthServiceTest.java` — Mockito para el caso de BD caída.

## Verificación

- `./mvnw test` — BUILD SUCCESS, 5 tests en verde.
- Arranque local: `GET /api/health/` → `{"status":"healthy","database":"connected","timestamp":"..."}`.