---
tipo: mejora
---

# Despliegue en Render: Docker, CORS y pooler transaccional

- **Fecha:** 2026-09-18
- **Área:** infraestructura
- **Estado:** hecha

## Descripción

Se preparó el backend para desplegarse en Render (plan free, 512 MB RAM / 0.1 CPU) como servicio web Docker, con CORS hacia el frontend de Vercel y la conexión a Supabase a través del pooler transaccional (que sí es alcanzable desde redes IPv4 como Docker/Render).

### Conexión a la base: dos vías verificadas

1. **Directo (local):** `db.akyvplcsiqhmfkqxvgoq.supabase.co:5432`, usuario `postgres`. Su DNS solo devuelve IPv6 (2600:1f14:...), por lo que solo funciona donde el host tenga IPv6. En Docker y Render falla con `Network is unreachable`.
2. **Pooler transaccional (Docker/Render):** `aws-0-us-west-2.pooler.supabase.com:6543` (región us-west-2, obtenida exponiendo varios candidatos), usuario `postgres.akyvplcsiqhmfkqxvgoq`, con `options=-c pooler_session_mode=transaction`. Es dual-stack/IPv4.
   - El hostname `aws-0-<region>.pooler.supabase.com` es compartido por región; el enrutado al proyecto real se hace con el usuario `postgres.<project-ref>`.
   - Se verificó conectividad con `psql` (us-east-1 → `ENOTFOUND tenant`, us-west-2 → `SELECT 'OK';`).

Se adoptó el estilo 12-factory: cada entorno aporta el JDBC URL completo (`DB_URL`), `DB_USER` y `DB_PASSWORD`; `application.properties` solo los resuelve como `spring.datasource.*`. Así la URL del pooler (con `options=...`) y la directa no se mezclan en el código.

## Cambios

- `Dockerfile` — multi-stage: build con `maven:3.9-eclipse-temurin-21` (`mvn dependency:go-offline` + `clean package -DskipTests`); runtime con `eclipse-temurin:21-jre`, usuario no-root `appuser` (uid 10001), `ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]`, `EXPOSE 8080`.
- `.dockerignore` — excluye `target/`, `.env*`, `boveda/`, `.git/`, `.idea/`, `.github/`, `.mvn/`, `HELP.md`, `mvnw`, `README*`, `*.md` (conserva `!render.yaml`): los secretos nunca entran a la imagen.
- `render.yaml` — blueprint web (runtime docker, región `oregon`, plan free, branch `main`, `healthCheckPath: /api/health`, envVars `DB_URL`/`DB_USER` pooler + `JAVA_OPTS=-XX:MaxRAMPercentage=60`; `DB_PASSWORD: sync: false` para fijarla a mano en el dashboard).
- `config/CorsConfig.java` — WebMvcConfigurer: `allowedOriginPatterns` desde `app.cors.allowed-origins` (`https://*.vercel.app,http://localhost:5173`), `allowCredentials(true)`, aplicado a `/api/**`.
- `application.properties` — `server.port=${PORT:8080}`, bloque CORS, `spring.datasource.url=${DB_URL}`, `spring.datasource.username=${DB_USER}`, `spring.datasource.password=${DB_PASSWORD}`.
- `.env` / `.env.example` — pasan de `DB_HOST`/`DB_PORT`/`DB_NAME` a `DB_URL` + `DB_USER` + `DB_PASSWORD` (directo en local).

## Verificación

- `./mvnw test` — BUILD SUCCESS (5 tests).
- `./mvnw package` — jar ejecutable generado.
- Arranque local con URL directa (IPv6) y con pooler: `GET /api/health/` → `healthy`/`connected`.
- `docker build -t pde-backend:verify .` OK y contenedor `docker run` con variables del pooler:
  - `/api/health/` → `{"status":"healthy","database":"connected",...}`.
  - `docker exec whoami` → `appuser`.
  - Memoria ~324 MiB (cabe en el plan free).
  - Preflight CORS desde `https://planificador-eventos-frontend.vercel.app` → 200 con `Access-Control-Allow-Origin`, `Allow-Methods` y `Allow-Credentials: true`.
- Alerta: la imagen debe reconstruirse tras cambiar config; los primeros intentos en contenedor fallaron porque la imagen conservaba el `application.properties` anterior.