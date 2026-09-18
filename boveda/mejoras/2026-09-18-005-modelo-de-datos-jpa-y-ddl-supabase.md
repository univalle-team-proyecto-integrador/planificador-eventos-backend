---
tipo: mejora
---

# Modelo de datos: entidades JPA y DDL para Supabase

- **Fecha:** 2026-09-18
- **Área:** persistencia
- **Estado:** hecha

## Descripción

Se definió el modelo de datos del Planificador de Eventos (Bloque 1): las cuatro tablas del esquema (`tipo_evento`, `usuario`, `evento`, `subtarea`) como entidades JPA, DTOs con validación y el script DDL listo para ejecutar en Supabase. Estrategia acordada: DDL manual en el SQL Editor de Supabase, manteniendo `spring.jpa.hibernate.ddl-auto=validate` (Hibernate nunca altera la BD compartida).

## Cambios

- `model/TipoEvento.java` — catálogo de tipos de evento (`id_tipo_evento`, `nombre` único).
- `model/Usuario.java` — organizador (`email` único, `password_hash`, `nombre`, `limite_horas_diarias` con valor por defecto 6).
- `model/Evento.java` — `ManyToOne` a `Usuario` y `TipoEvento`; `fecha_creacion` se rellena en `@PrePersist`.
- `model/Subtarea.java` — `ManyToOne` a `Evento`; estado como enum mapeado a string (valores `pendiente/ejecutada/pospuesta` en minúsculas, acordes al CHECK del DDL); `nota_explicativa` como `text`.
- `model/EstadoSubtarea.java` — enum con los tres estados en minúsculas (coincide con el CHECK constraint).
- `model/dto/TipoEventoDTO.java`, `UsuarioDTO.java`, `EventoDTO.java`, `SubtareaDTO.java` — records con Jakarta Validation (`@NotBlank`, `@Email`, `@Min`, `@Max`, `@Size`); FKs expuestas como ids.
- `db/ddl-supabase.sql` — DDL + seed de `tipo_evento` con `ON CONFLICT DO NOTHING`, listo para pegar en Supabase. Usa `IF NOT EXISTS` y NOT NULL+DEFAULT acordes a las entidades (para que `validate` no falle por nullabilidad).
- `src/test/java/.../model/ModeloEntidadesTest.java` — prueba con H2 (perfil `test`, `ddl-auto=create-drop`) que persiste las cuatro entidades, verifica los conteos y los valores por defecto (limite=6, estado=`pendiente`).

## Verificación

- `./mvnw test` — BUILD SUCCESS, 7 tests en verde (5 previos + 2 nuevos).
- Validación real contra PostgreSQL: se levantó un `postgres:16-alpine` local (Docker), se aplicó `db/ddl-supabase.sql`, se arrancó la app con ese `DB_URL` y `ddl-auto=validate` — **0 errores de schema-validation** y `/api/health/` → `healthy`/`connected`. El DDL y las entidades son 100% compatibles.
- El contenedor de prueba se eliminó al terminar.

## Pendiente del usuario

- Ejecutar `db/ddl-supabase.sql` en el **SQL Editor de Supabase** para crear las tablas en la BD compartida.