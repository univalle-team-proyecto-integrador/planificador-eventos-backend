# planificador-eventos-backend

Spring Boot 4.1.1 backend (Java 21, Maven) for the "Planificador de Eventos" project — sibling of `planificador-eventos-frontend`. Stack: JPA + PostgreSQL, Lombok, Validation, Actuator, springdoc-openapi, spring-dotenv. Team language: comments and commit messages in Spanish (mirroring the frontend repo).

## Commands

- `./mvnw spring-boot:run` — dev server (loads `.env` automatically via spring-dotenv)
- `./mvnw test` — run tests (uses H2 through the `test` profile; no network needed)
- `./mvnw package` — build executable jar in `target/`
- No linter or formatter configured; files use 4-space indent (don't add checkstyle/spotless config without asking)

## Database setup

- Runtime connects to Supabase/PostgreSQL. Every environment provides a full JDBC URL plus credentials via env vars (12-factor): `DB_URL`, `DB_USER`, `DB_PASSWORD`. `application.properties` resolves them as `spring.datasource.*`. Local dev reads them from `.env` (gitignored) via `me.paulschwarz:springboot4-dotenv`. **Never commit the real `.env` or real secrets** — `.env.example` only has placeholders.
- Two connection paths exist (see `boveda/mejoras/2026-09-18-004-deploy-render-docker-y-cors.md`):
  - Directo (local): `DB_URL=jdbc:postgresql://db.akyvplcsiqhmfkqxvgoq.supabase.co:5432/postgres?sslmode=require`, `DB_USER=postgres`. **IPv6-only** — only works where the host has IPv6; fails on Docker/Render.
  - Pooler transaccional (Docker/Render, IPv4): `DB_URL=jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&options=-c%20pooler_session_mode%3Dtransaction`, `DB_USER=postgres.akyvplcsiqhmfkqxvgoq`. Region is **us-west-2** (verified).
- Real env vars override `.env` (dotenv is added last).
- Tests use the `test` profile (`src/test/resources/application-test.properties`) with embedded H2 in PostgreSQL mode — `@ActiveProfiles("test")` must be present on any `@SpringBootTest`.
- `spring.jpa.hibernate.ddl-auto=validate` — never alters the shared remote DB; schema is defined manually in `db/ddl-supabase.sql` (run it once in the Supabase SQL Editor). Keep the DDL in sync with the JPA entities in `model/`; validated against a real PostgreSQL before changes.

## Gotchas (verified)

- **Spring Boot 4.x renamed starters** — do NOT "fix" `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation` back to Boot 3 names (`spring-boot-starter-web`, `spring-boot-starter-test`). Test bundles use a `-test` suffix (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, ...). Renaming them breaks the build.
- Local default JDK is 26; project targets Java 21 via `<java.version>21</java.version>` (compiles under 26 with `--release 21`).
- `HELP.md` is Spring Initializr boilerplate — ignore; `.gitignore` already excludes it from git.
- springdoc UI (when running): `/swagger-ui.html` (redirect → `/swagger-ui/index.html`), OpenAPI JSON at `/v3/api-docs`. `OpenApiConfig` publishes metadata and uses the current host as a relative server, so the same spec works locally and on Render.
- PostgreSQL driver is `runtime` scope. `spring-boot-devtools` is active in dev (auto-restart).
- PID cleanup note: Ctrl-C does not always kill child JVM on exit; use `pkill -9 -f PlanificadorEventosBackendApplication` if stray instances linger.

## Deploy (Render)

- Deployment is a Docker web service via the `render.yaml` blueprint (no Docker inside Render): repo `univalle-team-proyecto-integrador/planificador-eventos-backend`, branch `main`, region `oregon`, free plan (512 MB RAM / 0.1 CPU).
- `Dockerfile` (multi-stage `maven:3.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre` runtime) runs as non-root `appuser` (uid 10001) and launches `java $JAVA_OPTS -jar app.jar`. `JAVA_OPTS=-XX:MaxRAMPercentage=60` keeps the JVM under the 512 MB limit (~324 MB observed).
- Render injects `PORT`; `application.properties` uses `server.port=${PORT:8080}`. Health check path is `/api/health`; service URL is `https://planificador-eventos-backend.onrender.com`.
- Env vars for Render come from `render.yaml`: `DB_URL`/`DB_USER` (pooler, see Database setup) and `JAVA_OPTS`; `DB_PASSWORD` is `sync: false` — set it manually in the Render dashboard (never commit it). Blueprint is NOT auto-sync'd: after pushing, create the service via "New → Blueprint".
- `.dockerignore` excludes `.env*`, `boveda/`, tests and tooling from the image; secrets never enter it.
- CORS: `app.cors.allowed-origins=https://*.vercel.app,http://localhost:5173` in `application.properties`, enforced for `/api/**` by `config/CorsConfig.java` (allowedOriginPatterns + allowCredentials).
- Local Docker smoke test (before pushing): `docker build -t pde-backend:verify . && docker run -d --name pde-verify -e PORT=10000 -e DB_URL="<pooler>" -e DB_USER=postgres.akyvplcsiqhmfkqxvgoq -e DB_PASSWORD=<pw> -e JAVA_OPTS=-XX:MaxRAMPercentage=60 -p 10000:10000 pde-backend:verify`, then `curl localhost:10000/api/health/`.

## Structure

- Base package `uv.isj.planificadoreventosbackend`; layered packages:

  - `controller/` — REST controllers (views are JSON/OpenAPI, no JSP) — `HealthController`, `EventoController`, `SubtareaController`
  - `service/` — business logic — `HealthService`, `EventoService`, `SubtareaService`
  - `repository/` — Spring Data JPA — `EventoRepository`, `SubtareaRepository`, `TipoEventoRepository`, `UsuarioRepository`
  - `exception/` — domain exceptions and `GlobalExceptionHandler` (ProblemDetail responses)
  - `model/` — JPA entities (`TipoEvento`, `Usuario`, `Evento`, `Subtarea`), `EstadoSubtarea` enum, DTOs in `model/dto/`
  - `config/` — filters, interceptors, CORS and OpenAPI — `CorsConfig`, `OpenApiConfig`

- Spring routes live in `@RestController` + `@RequestMapping` annotations — there's no `routes/` dir (this is a Spring Boot repo, not Node/Express).
- `boveda/` — Obsidian vault with per-improvement records; see AGENTS section below.

## Obsidian vault (workflow requirement)

- `boveda/` is an Obsidian vault tracked in this repo. Every improvement you make (feature, bugfix, refactor, doc change) MUST be recorded there in the same session it is made.
- Create one dated file per improvement in `boveda/mejoras/` following `boveda/mejoras/plantilla-mejora.md` (filename `AAAA-MM-DD-NNN-breve-slug.md`), then update the index `boveda/mejoras/README.md`.
- Keep `boveda/lienzo-maestro.canvas` (Obsidian master canvas) current: every mejora gets a node linked to its record file.
- Vault notes, like code, are written in Spanish.

## References

- `README.md` — project overview (Spanish, mirrors the frontend README).