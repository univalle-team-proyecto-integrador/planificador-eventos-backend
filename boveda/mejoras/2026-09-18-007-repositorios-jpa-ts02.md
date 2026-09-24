---
tipo: mejora
---

# Repositorios JPA: TS02-BE-01 (consultas derivadas por entidad)

- **Fecha:** 2026-09-18
- **Área:** persistencia
- **Estado:** hecha

## Descripción

Se implementó la parte pendiente de TS02-BE-01: las interfaces `JpaRepository` para gestionar la persistencia de las cuatro entidades (descentralización: un repositorio por clase). TS02 pedía `Evento`, `Subtarea` y `TipoEvento`; se incluyó además `Usuario` porque `Evento` necesita resolver su FK `id_usuario` al filtrar por organizador.

## Cambios

- `repository/UsuarioRepository.java` — `findByEmail` (clave natural única) y `existsByEmail`.
- `repository/TipoEventoRepository.java` — `findByNombre` (nombre único).
- `repository/EventoRepository.java` — `findByUsuario_IdUsuario` (eventos por FK de organizador).
- `repository/SubtareaRepository.java` — `findByEvento_IdEvento` (subtareas por FK de evento) y `findByEstado`.
- `src/test/java/.../repository/RepositoriosTest.java` — smoke test con H2 (perfil `test`, `@Transactional`) que persiste vía repos y verifica las consultas derivadas.

## Gotcha registrado

Spring Data no alía `id` → propiedad `@Id` cuando la propiedad real no se llama `id` (`idUsuario`/`idEvento`). `findByUsuarioId` falla al partir `Id` (busca `usuario.Id`) y `findByUsuario_Id` tampoco resuelve (no existe la propiedad `id` en `Usuario`). La forma correcta es nombrar la propiedad real del destino: `findByUsuario_IdUsuario`. Queda documentado para futuras consultas anidadas.

## Verificación

- `./mvnw test` — BUILD SUCCESS, 11 tests (4 nuevos en `RepositoriosTest`).
- `ddl-auto=validate` no se ve afectado: los repositorios no tocan el esquema.

## Pendiente

- Cuando se decidan los endpoints, el patrón acordado es `Controller → Repository` directo (sin capa service por ahora), devolviendo los DTO records existentes.