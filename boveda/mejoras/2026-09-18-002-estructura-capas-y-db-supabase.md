---
tipo: mejora
---

# Estructura por capas y conexión a Supabase

- **Fecha:** 2026-09-18
- **Área:** infraestructura / persistencia
- **Estado:** hecha

## Descripción

Se organizó el proyecto en capas estilo Spring Boot (controller, service, repository, model, config), se añadió soporte de `.env` para credenciales, se configuró `application.properties` completo (DataSource Supabase, pool HikariCP, JPA/Hibernate) y un perfil de test con H2 para que los tests corran sin red.

## Cambios

- `src/main/java/uv/isj/planificadoreventosbackend/{controller,service,repository,model,config}/` — carpetas por capa (con `.gitkeep`).
- `.gitignore` — excluye `.env`, `.env.*` y variantes; permite `.env.example`.
- `.env` (gitigrrado) — credenciales reales de Supabase (host, puerto, BD, usuario, password). Decisión de equipo: **se usa la base `postgres`** de Supabase (esquema `public`), porque `ProyectoIntegrador` no existe aún en el proyecto y `postgres` ya está verificada.
- `.env.example` — plantilla sin secretos para el equipo.
- `pom.xml` — dependencias `me.paulschwarz:springboot4-dotenv` (BOM 5.1.0) y `com.h2database:h2` (test).
- `src/main/resources/application.properties` — DataSource `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require`, pool Hikari (máx. 10, timeout 30s, max-lifetime 30min), `spring.jpa.open-in-view=false`, `ddl-auto=validate`, Actuator health/info y Swagger UI.
- `src/test/resources/application-test.properties` — H2 en modo PostgreSQL.
- `PlanificadorEventosBackendApplicationTests` — `@ActiveProfiles("test")`.
- `AGENTS.md` — sección Database setup, estructura por capas y gotchas actualizados.

## Verificación

- `./mvnw test` → BUILD SUCCESS (1 test, perfil H2, sin red).
- `./mvnw spring-boot:run` con `.env` por defecto (`DB_NAME=postgres`) → arranca, `/actuator/health` = `UP`, `/v3/api-docs` = 200, Swagger = 200.
- Con `DB_NAME=ProyectoIntegrador` → `FATAL: database "ProyectoIntegrador" does not exist`: la conexión y autenticación funcionan, pero esa BD no existe (se descartó en favor de `postgres`).
- `git check-ignore .env` → ignorado; `.env.example` → versionable.