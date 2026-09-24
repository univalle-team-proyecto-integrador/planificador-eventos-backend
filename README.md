# Planificador de Eventos — Backend

Backend del MVP (Sprint 1) para el planificador de eventos dirigido a organizadores independientes. Expone una API REST de Spring Boot que da soporte al frontend React/Vite: gestión de eventos, subtareas del plan logístico y control del límite de horas diarias del organizador.

- Repositorio del frontend: `planificador-eventos-frontend` (deploy en Vercel)

## Stack

- **Spring Boot 4.1.1 + Java 21 + Maven** (wrapper `./mvnw`, target `--release 21`)
- **Spring Data JPA / Hibernate + PostgreSQL** (Supabase)
- **Lombok**, **Bean Validation**, **Actuator**, **springdoc-openapi**
- **spring-dotenv** (`me.paulschwarz:springboot4-dotenv`) — carga `.env` en desarrollo
- **H2 embebida** en el perfil de tests (`@ActiveProfiles("test")`)

## Comandos

| Comando                 | Descripción                                                    |
| ----------------------- | -------------------------------------------------------------- |
| `./mvnw spring-boot:run`| Levanta el servidor de desarrollo (carga `.env` automáticamente)|
| `./mvnw test`           | Ejecuta los tests con H2 (perfil `test`, sin red)              |
| `./mvnw package`        | Genera el jar ejecutable en `target/`                           |

No hay linter ni formatter configurado; los archivos usan indentación de 4 espacios.

## Configuración del entorno

La configuración es de estilo **12-factor**: cada entorno aporta el JDBC URL completo y las credenciales vía variables de entorno. La aplicación las resuelve como `spring.datasource.*`.

1. Copiar `.env.example` a `.env` y rellenar los valores.
2. Variables requeridas:

| Variable      | Descripción                                      |
| ------------- | ------------------------------------------------ |
| `DB_URL`      | JDBC URL completo de la base (incluye `sslmode`) |
| `DB_USER`     | Usuario de la base de datos                      |
| `DB_PASSWORD` | Contraseña de la base de datos                   |

Existen **dos vías de conexión** a Supabase (ver `boveda/mejoras/2026-09-18-004-deploy-render-docker-y-cors.md`):

- **Directa (local)** — `db.<ref>.supabase.co:5432` con usuario `postgres`. El DNS solo resuelve **IPv6**, así que funciona solo donde el host tenga IPv6 (falla en Docker/Render).
- **Pooler transaccional (Docker/Render)** — `aws-0-us-west-2.pooler.supabase.com:6543` con usuario `postgres.<ref>`. Es IPv4/dual-stack, alcanzable desde cualquier red. Requiere el parámetro `options=-c%20pooler_session_mode%3Dtransaction` en el `DB_URL`.

> **Nunca** commitear `.env` ni secretos reales. `.gitignore` y `.dockerignore` los excluyen; `.env.example` solo tiene placeholders.

## Estructura del proyecto

Base package `uv.isj.planificadoreventosbackend`, organizado en capas al estilo Spring:

```
src/main/java/uv/isj/planificadoreventosbackend/
├── controller/     # HealthController, EventoController, SubtareaController
├── service/        # HealthService, EventoService, SubtareaService
├── repository/     # Repositorios Spring Data JPA por entidad
├── exception/      # Excepción de dominio y manejador global de errores
├── model/          # Entidades JPA (TipoEvento, Usuario, Evento, Subtarea, EstadoSubtarea)
│   └── dto/        # DTOs con validación
└── config/         # Configuración global (CorsConfig)
db/
└── ddl-supabase.sql # DDL ejecutado en Supabase (esquema + seed)
```

## API

| Método   | Endpoint                            | Descripción                                                      |
| -------- | ----------------------------------- | ---------------------------------------------------------------- |
| `GET`    | `/api/health`                       | Estado de la aplicación y de la base; 503 si la BD no responde  |
| `GET`    | `/api/eventos`                      | Lista los eventos                                               |
| `GET`    | `/api/eventos/{id}`                 | Obtiene el detalle de un evento                                |
| `POST`   | `/api/eventos`                      | Crea un evento; devuelve 201 y la ubicación del recurso          |
| `POST`   | `/api/eventos/{id}/subtareas`       | Agrega una subtarea; devuelve 201                                |
| `DELETE` | `/api/eventos/{id}`                 | Elimina el evento y sus subtareas en cascada; devuelve 204        |
| `PATCH`  | `/api/subtareas/{id}/reprogramar`   | Reprograma y devuelve el conflicto de límite diario, si existe   |
| `PATCH`  | `/api/subtareas/{id}/estado`        | Cambia el estado a `ejecutada` o `pospuesta`                     |
| `GET`    | `/swagger-ui.html`                  | Documentación OpenAPI (Swagger UI)                               |
| `GET`    | `/v3/api-docs`                      | JSON de la especificación OpenAPI                                |

### Documentación interactiva

Swagger UI se genera desde el propio backend con springdoc. No se conecta a `https://swagger.io/product/why-swagger/`: esa URL es informativa; la interfaz de esta API vive en el servidor del proyecto.

- **Local:** `http://localhost:8080/swagger-ui.html`
- **Local (OpenAPI JSON):** `http://localhost:8080/v3/api-docs`
- **Render:** `https://planificador-eventos-backend.onrender.com/swagger-ui.html`
- **Render (OpenAPI JSON):** `https://planificador-eventos-backend.onrender.com/v3/api-docs`

La opción **Try it out** está habilitada. Como la especificación usa un servidor relativo, Swagger envía las solicitudes al mismo host desde el que se abrió la interfaz, tanto en localhost como en Render.

Ejemplo de respuesta de `/api/health`:

```json
{
  "status": "healthy",
  "database": "connected",
  "timestamp": "2026-09-18T04:00:54-05:00"
}
```

## Base de datos (Supabase)

- Esquema creado a mano en el SQL Editor de Supabase con `db/ddl-supabase.sql` (ya ejecutado y verificado).
- `spring.jpa.hibernate.ddl-auto=validate` — la app **nunca** altera el esquema remoto; se valida contra él al arrancar.
- Tablas: `tipo_evento` (catálogo, 5 tipos semilla), `usuario` (organizador, límite de 1–16 h/día), `evento`, `subtarea` (estados `pendiente`/`ejecutada`/`pospuesta`).

## Despliegue (Render)

- Servicio web **Docker**, blueprint `render.yaml` (región `oregon`, plan free, branch `main`, health check `/api/health`).
- `Dockerfile` multi-stage (build `maven:3.9-eclipse-temurin-21` → runtime `eclipse-temurin:21-jre`) que corre como usuario no-root `appuser` (uid 10001).
- Variables de entorno en Render: `DB_URL`/`DB_USER` (**pooler transaccional**), `JAVA_OPTS=-XX:MaxRAMPercentage=60`; `DB_PASSWORD` se fija a mano en el dashboard (nunca en el repo).
- CORS: `app.cors.allowed-origins=https://*.vercel.app,http://localhost:5173`, aplicado a `/api/**` por `config/CorsConfig.java`.
- URL pública: `https://planificador-eventos-backend.onrender.com`

> **Estado actual (24 de septiembre de 2026):** la URL pública `https://planificador-eventos-backend.onrender.com` responde con otro servicio **Django REST Framework** (`/api/health` devuelve `status: ok`), no con esta aplicación Spring. `/v3/api-docs` y `/swagger-ui.html` devuelven 404. Se debe crear o corregir el servicio Render de este repositorio, configurar `DB_PASSWORD` en el dashboard y volver a desplegar antes de usar la documentación pública.

## Estado y hoja de ruta

**Hecho**

- Infraestructura: capas Spring, conexión a Supabase (vía directa y pooler), CORS, Docker + `render.yaml`
- Endpoint `/api/health` con verificación real de la base
- Modelo de datos: entidades JPA, DTOs y DDL para Supabase, verificado contra PostgreSQL real
- Repositorios JPA con consultas por usuario, evento, fecha y estado
- Servicios de eventos y subtareas con validación, mapeo de relaciones y control de límite diario
- API REST de eventos y subtareas con validación, cascada y manejo global de errores

**Pendiente**

- Exponer mediante endpoints la consulta JPQL de la vista «Hoy» ya disponible en el repositorio
- Autenticación (JWT) — fuera de alcance por ahora

Cada mejora queda registrada en la bóveda Obsidian del repo (`boveda/mejoras/`).

## Convenciones

- Textos, comentarios y mensajes de commit en español (espejo del frontend)
- Indentación de 4 espacios; sin linter/formatter
- Guía para agentes de desarrollo: consultar `AGENTS.md`
- Registro de mejoras: bóveda Obsidian en `boveda/`

## Flujo de ramas

- La rama `backend/lead` es la rama de desarrollo: allí cada desarrollador realiza su proceso de trabajo.
- La rama `main` es la raíz coordinada por la persona responsable de coordinación o QA. Los cambios se agregan únicamente desde `main`, después de revisar y validar el trabajo desarrollado en `backend/lead`.

## Autores

- Santiago Ruiz Gallego
- Juan Garcia Durango
- Isaac Antonio Antillano Cruiz