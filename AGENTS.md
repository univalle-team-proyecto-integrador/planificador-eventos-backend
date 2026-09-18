# planificador-eventos-backend

Spring Boot 4.1.1 backend (Java 21, Maven) for the "Planificador de Eventos" project — sibling of `planificador-eventos-frontend`. Stack: JPA + PostgreSQL, Lombok, Validation, Actuator, springdoc-openapi. Team language: comments and commit messages in Spanish (mirroring the frontend repo).

## Commands

- `./mvnw spring-boot:run` — dev server
- `./mvnw test` — run tests (`target/surefire-reports/` on failure)
- `./mvnw package` — build executable jar in `target/`
- No linter or formatter configured; files use 4-space indent (don't add checkstyle/spotless config without asking)

## Gotchas (verified)

- **Tests currently fail**: `contextLoads` aborts with `Failed to determine a suitable driver class`. The JPA starter is on the classpath but `src/main/resources/application.properties` only sets `spring.application.name` — no DataSource URL. Point at a PostgreSQL (or add an embedded DB for tests) before `./mvnw test` can pass.
- **Spring Boot 4.x renamed starters** — do NOT "fix" `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation` back to Boot 3 names (`spring-boot-starter-web`, `spring-boot-starter-test`). Test bundles use a `-test` suffix (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, ...). Renaming them breaks the build.
- Local default JDK is 26; project targets Java 21 via `<java.version>21</java.version>` (compiles under 26 with `--release 21`).
- `HELP.md` is Spring Initializr boilerplate — ignore; `.gitignore` already excludes it from git.
- springdoc UI (when running): `/swagger-ui/index.html`, OpenAPI JSON at `/v3/api-docs`.

## Structure

- Base package `uv.isj.planificadoreventosbackend`; currently only the `@SpringBootApplication` bootstrap class and a `@SpringBootTest` context-load test. No domain code yet.
- PostgreSQL driver is `runtime` scope; no DB config and no migrations setup yet.

## Obsidian vault (workflow requirement)

- `boveda/` is an Obsidian vault tracked in this repo. Every improvement you make (feature, bugfix, refactor, doc change) MUST be recorded there in the same session it is made.
- Create one dated file per improvement in `boveda/mejoras/` following `boveda/mejoras/plantilla-mejora.md` (filename `AAAA-MM-DD-NNN-breve-slug.md`), then update the index `boveda/mejoras/README.md`.
- Keep `boveda/lienzo-maestro.canvas` (Obsidian master canvas) current: every mejora gets a node linked to its record file.
- Vault notes, like code, are written in Spanish.

## References

- `README*` — none in this repo yet (frontend keeps its own `AGENTS.md` in the sibling repo).