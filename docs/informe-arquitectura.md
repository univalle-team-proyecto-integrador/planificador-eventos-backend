# Informe de análisis y arquitectura — Planificador de Eventos (backend)

- **Fecha:** 2026-10-10
- **Alcance:** repositorio `planificador-eventos-backend` completo (código, DDL, seguridad, tests, scripts de despliegue, configuración y bóveda Obsidian)
- **Rol:** investigador + arquitecto de software
- **Estado:** documento de referencia; la priorización P0→P2 es la hoja de ruta sugerida

---

## Resumen ejecutivo

Proyecto **Spring Boot 4.1.1 / Java 21** bien encaminado para un MVP de Sprint 1: arquitectura por capas limpia, DTOs como `record`, validación en dos niveles, manejo de errores homogéneo con `ProblemDetail` (RFC 7807), autenticación JWT *stateless* y un aislamiento por propietario poco habitual pero muy bien pensado y cubierto por pruebas. La documentación (README + bóveda Obsidian + scripts de verificación de despliegue) está por encima del promedio y da trazabilidad real a cada decisión.

El informe identifica **2 hallazgos críticos** (brecha de autenticación en `/api/eventos/**` e inconsistencia de zona horaria en la vista "Hoy"), varios hallazgos de arquitectura/rendimiento (N+1, falta de migraciones versionadas e índices, contratos sin tipar) y un roadmap priorizado para atacarlos.

## Descripción general

- **Stack:** Spring Boot 4.1.1 + Java 21 (target `--release 21`), Spring Data JPA/Hibernate + PostgreSQL (Supabase), Lombok, Bean Validation, Actuator, springdoc-openapi, spring-dotenv. Despliegue en Render vía Docker `render.yaml`.
- **Base package:** `uv.isj.planificadoreventosbackend`, capas `controller/`, `service/`, `repository/`, `security/`, `exception/`, `model/` (+ `dto/`), `config/`.
- **Dominio:** organizadores independientes gestionan eventos, subtareas del plan logístico y el límite de horas diarias.
- **API:** eventos, subtareas (vista "Hoy", reprogramación con control de límite), catálogo de tipos, salud y autenticación US-11 (registro, login, perfil).

## Fortalezas (a conservar)

1. **Aislamiento por propietario sólido.** `EventoRepository` y `SubtareaRepository` extienden `Repository` (marcador vacío) y **no** `JpaRepository`/`CrudRepository`, de modo que `findAll()` y `findById(id)` no existen y no se puede leer/modificar una cuenta ajena por descuido:
   - `src/main/java/.../repository/EventoRepository.java:16`
   - `src/main/java/.../repository/SubtareaRepository.java:33`
   - Verificado por reflexión en `EventoRepositoryAislamientoTest` y `SubtareaRepositoryAislamientoTest`.
2. **No se filtra la existencia de recursos ajenos.** Un evento/subtarea de otra cuenta responde **404**, no 403, para no confirmar que el id existe (`service/EventoService.java:111-119`, `service/SubtareaService.java:169-173`).
3. **Configuración 12-factor y despliegue automatizado.** Todo se resuelve por variables de entorno (`application.properties`), `render.yaml` + `Dockerfile` multi-stage en usuario no-root (uid 10001), y dos scripts que validan la cadena completa: `scripts/verificar-despliegue.sh` y `scripts/smoke-imagen.sh`.
4. **Errores homogéneos.** `GlobalExceptionHandler` produce `ProblemDetail` y el `RestAuthenticationEntryPoint` (`security/RestAuthenticationEntryPoint.java:18`) escribe el 401 de la cadena de filtros como JSON, el fallo clásico que se suele olvidar (los `@RestControllerAdvice` no interceptan la cadena de seguridad).
5. **Sin secretos versionados.** `.env` está en `.gitignore`/`.dockerignore` y fuera del historial de git; `render.yaml` deja `DB_PASSWORD` y `JWT_SECRET` como `sync: false`.
6. **Trazabilidad de mejora.** Bóveda Obsidian (`boveda/mejoras/`) con record por mejora, índice y lienzo maestro actualizados.

## Hallazgos críticos

### 1. `/api/eventos/**` queda abierto en la cadena de seguridad (fail-open)

No hay `requestMatchers` para `/api/eventos/**` en `config/SecurityConfig.java:95-108`, así que esas rutas caen en `anyRequest().permitAll()`. Sin token, `EventoController.propietario()` delega en `CurrentUserProvider.idUsuarioActual()`, que devuelve `LEGACY_USER_ID` (por defecto `1`).

**Consecuencia con la configuración actual de Render** (`PROTECT_SUBTAREAS=false`): cualquiera puede leer, crear, editar y borrar los eventos del usuario legado **sin token**. El README y AGENTS.md afirman aislamiento por token para eventos, pero la autenticación no se exige en la cadena.

Además hay un **acoplamiento oculto**: el flag `app.security.protect-subtareas` (llamado "subtareas") termina gateando también `/api/eventos`, porque `CurrentUserProvider.idUsuarioLegadoSiProcede()` lanza `SinAutenticacionException` (→ 401) cuando el flag está en `true` (`security/CurrentUserProvider.java:74-84`).

**Sugerencia (P0):**
- Añadir un matcher explícito para `/api/eventos/**` (`.authenticated()` o un `AuthorizationManager` propio).
- Cambiar el default de la cadena a **default-deny** (`anyRequest().authenticated()`) + lista blanca de rutas públicas, en lugar de `permitAll`.
- Renombrar/separar el flag: `app.security.require-auth` (global) con compatibilidad opcional, para que el nombre no mienta sobre su alcance.

### 2. Inconsistencia de zona horaria en la vista "Hoy"

- `SubtareaService.obtenerParaHoy` usa `LocalDate.now(ZoneId.of("America/Bogota"))` (`service/SubtareaService.java:51`).
- `SubtareaService.obtenerHoyAgrupado` usa `LocalDate.now()` (`service/SubtareaService.java:180`), que depende de la zona del JVM (en Render es UTC).

Dos endpoints de la misma vista pueden devolver **días distintos** (desfase de 5 horas → un día). Se suma el uso de `LocalDateTime.now()` sin zona para `fecha_creacion` (`model/Evento.java:63`, `model/Subtarea.java:59`) con columnas `TIMESTAMP` y `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`.

**Sugerencia (P0/P1):**
- Inyectar un `Clock` fijo con la zona del negocio (`America/Bogota`) o configurar `user.timezone`/`JAVA_OPTS -Duser.timezone`.
- Migrar fechas a `Instant`/`OffsetDateTime` con columnas `TIMESTAMPTZ`.

## Hallazgos de arquitectura y rendimiento

- **N+1 en consultas de subtareas.** `findNoEjecutadasParaHoy` y `findNoEjecutadasHastaFecha` (`repository/SubtareaRepository.java:62-98`) no traen `JOIN FETCH s.evento`, y `SubtareaService.aDto` invoca `subtarea.getEvento().getIdEvento()` (`service/SubtareaService.java:266-276`). Con `spring.jpa.open-in-view=false` se resuelve dentro de la transacción, pero dispara una consulta extra por subtarea. Añadir `JOIN FETCH` o una proyección DTO.
- **Sin migraciones versionadas.** El esquema vive en `db/ddl-supabase.sql` (manual) y los tests usan H2 `create-drop`, que **no valida** contra el DDL real. Un cambio de entidad puede pasar tests y romper producción. Flyway/Liquibase encajaría bien.
- **Sin índices en columnas de filtro.** PostgreSQL no indexa FKs automáticamente: conviene índices en `evento(id_usuario)`, `evento(id_tipo_evento)`, `subtarea(id_evento)` y `subtarea(fecha_objetivo)`.
- **`reprogramar` devuelve un `Map<String,Object>` sin tipar** (`service/SubtareaService.java:105-146`, `controller/SubtareaController.java:135-144`). Rompe el contrato OpenAPI tipado del resto de la API. Debería ser un `record` (p. ej. `ReprogramacionResponseDTO`).
- **Condición de carrera (TOCTOU) en el límite diario.** `obtenerLimiteDiario` + `reprogramar` + `sumarHorasNoEjecutadasPorFechaYUsuario` se ejecutan sin lock; dos reprogramaciones concurrentes pueden exceder el límite (`controller/SubtareaController.java:139-142`).
- **Doble carga de la misma subtarea** en la reprogramación: `obtenerLimiteDiario` y `reprogramar` hacen dos `findByIdYUsuarioId` para el mismo recurso.
- **Sin paginación** en `GET /api/eventos` ni `/api/subtareas/hoy/agrupado`, y `findNoEjecutadasHastaFecha` usa una ventana fija de 30 días no documentada (`service/SubtareaService.java:183`).
- **Observabilidad mínima.** Actuator expone solo `health,info` sin `show-details`, sin Micrometer/Prometheus ni logs estructurados con correlation id.
- **Doble matcher de CORS en dos capas** (MVC y cadena de seguridad) con una solo fuente de verdad: correcto, pero cada cambio debe tocar el mismo lugar.

## Calidad, seguridad y mantenibilidad (menores)

- Sin **rate limiting ni bloqueo de cuenta** en `POST /api/users/login` (expone fuerza bruta). BCrypt (cost 10) es adecuado, pero el token de 8 h no tiene **refresh ni revocación/logout**.
- `GlobalExceptionHandler` no cubre `DataIntegrityViolationException` genérica ni un fallback 500 consistente (`exception/GlobalExceptionHandler.java`).
- Ruido: imports duplicados de `HoyResponseDTO` (`service/SubtareaService.java:18-19`, `controller/SubtareaController.java:28-29`), método sin uso `JwtService.instanteActual()` (`security/JwtService.java:79`), javadocs huérfanos/duplicados (`controller/SubtareaController.java:185-196`) e indentación irregular (`controller/SubtareaController.java:102`).
- `EventoDTO` mezcla request y response (`idEvento`, `fechaCreacion`, `subtareas` en modo lectura dentro del mismo record usado en el body). Separar request/response mejora el contrato y la documentación OpenAPI.

## Análisis como arquitecto

| Dimensión        | Valoración | Comentario |
| ---------------- | ---------- | ---------- |
| Capas y separación | **Bueno** | Controller → Service → Repository bien delimitado; DTOs como records inmutables. |
| Seguridad        | **Riesgo alto** | Diseño de aislamiento muy bueno, pero la cadena es *fail-open* y el flag `PROTECT_SUBTAREAS` gobierna más de lo que su nombre dice. |
| Datos/persistencia| **Aceptable** | `ddl-auto=validate` evita que la app altere el esquema, pero sin migraciones versionadas ni índices el mantenimiento y el rendimiento sufren. |
| Rendimiento      | **Aceptable** | Correcto para MVP; N+1 y falta de paginación limitan el crecimiento. |
| Despliegue       | **Bueno** | 12-factor, Docker multi-stage no-root, health check en `/api/health`, scripts de verificación y smoke. |
| Testing          | **Bueno** | 133 `@Test`; cobertura por capa y pruebas de aislamiento por reflexión. Limitación razonable: H2 ≠ PostgreSQL. |
| Observabilidad   | **Débil** | Actuator mínimo; sin métricas ni logging estructurado. |
| Mantenibilidad   | **Buena** | Código legible, comentarios útiles, documentación viva en la bóveda. Ruido menor (imports duplicados, javadocs). |

## Roadmap priorizado

### P0 — Seguridad y consistencia
1. Cerrar la brecha de `/api/eventos/**` y pasar a *default-deny* (`anyRequest().authenticated()` + lista blanca).
2. Separar/renombrar el flag de protección para que no acople el alcance de eventos y subtareas.
3. Unificar la zona horaria (`Clock`/`ZoneId` fijo) en toda la lógica de fechas.

### P1 — Datos y contratos
4. Migraciones versionadas (Flyway) en sincronía con `db/ddl-supabase.sql`.
5. Índices en FK y `subtarea(fecha_objetivo)`.
6. `JOIN FETCH`/proyección DTO en las consultas de subtareas (eliminar el N+1).
7. Tipar la respuesta de `reprogramar` (DTO) y marcar el desempate por `horasEstimadas`.
8. Rate limiting en `login` y manejar `DataIntegrityViolationException` en el handler global.

### P2 — Escalabilidad y observabilidad
9. Paginación en los listados y ventana configurable para "próximas".
10. Refresh tokens/revocación si el producto crece.
11. Actuator con `show-details`, Micrometer/Prometheus y logs estructurados.

## Referencias por archivo

- Skipped (sin cambios de código en este informe): se citan rutas y líneas a lo largo del documento.

---

*Informe generado a partir de la revisión completa del repositorio. Cada mejora futura debe registrarse en `boveda/mejoras/`.*