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

Además, desde US-11 la autenticación exige un secreto propio:

| Variable                 | Requerida | Descripción                                                                     |
| ------------------------ | --------- | ------------------------------------------------------------------------------- |
| `JWT_SECRET`             | sí        | Clave HMAC-SHA256. **Mínimo 32 caracteres**; la app no arranca sin ella          |
| `JWT_EXPIRATION_SECONDS` | no        | Caducidad del token en segundos (por defecto `28800`, 8 horas)                    |
| `PROTECT_SUBTAREAS`      | no        | Interruptor de despliegue de US-11 (`false` por defecto)                          |
| `LEGACY_USER_ID`         | no        | Usuario legado acotado mientras `PROTECT_SUBTAREAS=false` (por defecto `1`)      |

Generar el secreto con `openssl rand -base64 48`. En Render se deja `sync: false` y se fija a mano en el panel: **el valor real jamás se versiona**.

Existen **dos vías de conexión** a Supabase (ver `boveda/mejoras/2026-09-18-004-deploy-render-docker-y-cors.md`):

- **Directa (local)** — `db.<ref>.supabase.co:5432` con usuario `postgres`. El DNS solo resuelve **IPv6**, así que funciona solo donde el host tenga IPv6 (falla en Docker/Render).
- **Pooler transaccional (Docker/Render)** — `aws-0-us-west-2.pooler.supabase.com:6543` con usuario `postgres.<ref>`. Es IPv4/dual-stack, alcanzable desde cualquier red. Requiere el parámetro `options=-c%20pooler_session_mode%3Dtransaction` en el `DB_URL`.

> **Nunca** commitear `.env` ni secretos reales. `.gitignore` y `.dockerignore` los excluyen; `.env.example` solo tiene placeholders.

## Estructura del proyecto

Base package `uv.isj.planificadoreventosbackend`, organizado en capas al estilo Spring:

```
src/main/java/uv/isj/planificadoreventosbackend/
├── controller/     # HealthController, EventoController, SubtareaController, TipoEventoController
├── service/        # HealthService, EventoService, SubtareaService, TipoEventoService
├── repository/     # Repositorios Spring Data JPA por entidad
├── exception/      # Excepción de dominio y manejador global de errores
├── model/          # Entidades JPA (TipoEvento, Usuario, Evento, Subtarea, EstadoSubtarea)
│   └── dto/        # DTOs con validación
└── config/         # Configuración global (CorsConfig, OpenApiConfig)
db/
└── ddl-supabase.sql # DDL ejecutado en Supabase (esquema + seed)
```

## API

| Método   | Endpoint                            | Descripción                                                      |
| -------- | ----------------------------------- | ---------------------------------------------------------------- |
| `GET`    | `/api/health`                       | Estado de la aplicación y de la base; 503 si la BD no responde  |
| `POST`   | `/api/users/register`               | Crea una cuenta y devuelve el token; 409 si el correo ya existe |
| `POST`   | `/api/users/login`                  | Autentica y devuelve el token; 401 si las credenciales fallan  |
| `GET`    | `/api/users/profile`                | Devuelve el usuario del token; 401 sin cabecera `Bearer`        |
| `GET`    | `/api/tipos-evento`                | Lista el catálogo de tipos de evento                            |
| `GET`    | `/api/eventos`                     | Lista los eventos del usuario del token                          |
| `GET`    | `/api/eventos/{id}`                 | Obtiene el detalle de un evento                                |
| `GET`    | `/api/eventos/{id}/subtareas`       | Lista las subtareas de un evento                               |
| `GET`    | `/api/subtareas/hoy?fecha={yyyy-MM-dd}` | Lista las gestiones no ejecutadas para una fecha                |
| `POST`   | `/api/eventos`                      | Crea un evento; devuelve 201 y la ubicación del recurso          |
| `PUT`    | `/api/eventos/{id}`                 | Actualiza un evento                                             |
| `POST`   | `/api/eventos/{id}/subtareas`       | Agrega una subtarea; devuelve 201                                |
| `DELETE` | `/api/eventos/{id}`                 | Elimina el evento y sus subtareas en cascada; devuelve 204        |
| `GET`    | `/api/subtareas?eventoId={id}`      | Lista las subtareas de un evento                               |
| `GET`    | `/api/subtareas/{id}`               | Obtiene el detalle de una subtarea                              |
| `PUT`    | `/api/subtareas/{id}`               | Actualiza nombre, fecha objetivo y horas de una subtarea         |
| `PATCH`  | `/api/subtareas/{id}/reprogramar`   | Reprograma y devuelve el conflicto de límite diario, si existe   |
| `PATCH`  | `/api/subtareas/{id}/estado`        | Cambia el estado; `pendiente` reabre una subtarea                |
| `DELETE` | `/api/subtareas/{id}`               | Elimina una subtarea                                             |
| `GET`    | `/swagger-ui.html`                  | Documentación OpenAPI (Swagger UI)                               |
| `GET`    | `/v3/api-docs`                      | JSON de la especificación OpenAPI                                |

### Autenticación (US-11)

La API es **stateless con JWT**: no hay cookies ni sesiones en el servidor. Tras `register` o `login` hay que enviar el token en cada llamada:

```
Authorization: Bearer <token>
```

Login y registro aceptan `password` (no `contrasena`) y responden con el mismo cuerpo:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "usuario": {
    "idUsuario": 1,
    "email": "santiago@correo.com",
    "nombre": "Santiago Pérez",
    "limiteHorasDiarias": 8
  }
}
```

Rutas públicas (sin token): `/api/health`, `/api/tipos-evento`, `/api/users/register`, `/api/users/login`, `/v3/api-docs/**`, `/swagger-ui/**` y el preflight `OPTIONS`. En Swagger UI el botón **Authorize** usa el esquema `bearerAuth`.

### Aislamiento por propietario

Todos los datos se acotan al usuario del token. Los parámetros `usuarioId` del query y el campo `idUsuario` del cuerpo **se ignoran**, y ambos son opcionales:

- `GET /api/eventos` devuelve solo los eventos del token (antes devolvía todos).
- `POST /api/eventos` asigna el propietario desde el token, ignorando el `idUsuario` del cuerpo.
- `PUT /api/eventos/{id}` y `DELETE /api/eventos/{id}` no pueden tocar eventos ajenos: responden **404**, no 403, para no confirmar que el id existe en otra cuenta.
- `EventoRepository` y `SubtareaRepository` extienden `Repository`, no `JpaRepository`, para que `findAll()` y `findById(id)` **no existan** y no se pueda leer o borrar una cuenta ajena por descuido.

### Activación gradual

`PROTECT_SUBTAREAS` permite activar la protección sin romper el frontend:

| `PROTECT_SUBTAREAS` | Comportamiento                                                                    |
| ------------------- | --------------------------------------------------------------------------------- |
| `false` (por defecto) | `/api/subtareas/**` sigue abierta, pero los datos se acotan a `LEGACY_USER_ID`   |
| `true`              | `/api/subtareas/**` exige `Authorization: Bearer`; sin token responde 401           |

Orden de despliegue: subir el backend con la bandera en `false` → desplegar el frontend que ya manda el token → verificar → subir `PROTECT_SUBTAREAS=true`.

### Documentación interactiva

Swagger UI se genera desde el propio backend con springdoc. No se conecta a `https://swagger.io/product/why-swagger/`: esa URL es informativa; la interfaz de esta API vive en el servidor del proyecto.

- **Local:** `http://localhost:8080/swagger-ui.html`
- **Local (OpenAPI JSON):** `http://localhost:8080/v3/api-docs`
- **Render:** `https://planificador-eventos-backend-1.onrender.com/swagger-ui.html`
- **Render (OpenAPI JSON):** `https://planificador-eventos-backend-1.onrender.com/v3/api-docs`

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
- Variables de entorno en Render: `DB_URL`/`DB_USER` (**pooler transaccional**), `JAVA_OPTS=-XX:MaxRAMPercentage=60`, `JWT_EXPIRATION_SECONDS=28800`, `PROTECT_SUBTAREAS=false` y `LEGACY_USER_ID=1`; `DB_PASSWORD` y **`JWT_SECRET`** se fijan a mano en el dashboard (`sync: false`, nunca en el repo).
- El servicio **no arranca sin `JWT_SECRET`**: es el único secreto nuevo que hay que crear en el panel de Render (`openssl rand -base64 48`).
- CORS: `app.cors.allowed-origins=https://*.vercel.app,http://localhost:5173`, aplicado a `/api/**` por `config/CorsConfig.java`.
- URL pública: `https://planificador-eventos-backend-1.onrender.com` (servicio Spring de este repositorio).

> **Estado actual (24 de septiembre de 2026):** verificado de punta a punta. El servicio Spring del repositorio está desplegado y funcionando en `https://planificador-eventos-backend-1.onrender.com`: `/api/health` responde `healthy`/`connected`, `/v3/api-docs` y `/swagger-ui.html` OK, y los endpoints devuelven datos reales de Supabase. El dominio `https://planificador-eventos-backend.onrender.com` (sin sufijo) sigue sirviendo otra API (no Spring, `/api/health` → `{"status":"ok",...}` y 404 en `/v3/api-docs`): es un servicio ajeno/heredado, no usar.
>
> Frontend de referencia: `https://planificador-eventos-frontend-ten.vercel.app` (Vercel, SPA Vite). Su `VITE_API_URL` apunta a `https://planificador-eventos-backend-1.onrender.com`.

### Ruta para verificar la conexión tras un despliegue

Después de cualquier cambio y redeploy del backend o del frontend, ejecutar la verificación de la cadena completa en un solo comando:

```bash
bash scripts/verificar-despliegue.sh
```

El script comprueba (en orden):

1. **Backend vivo:** `GET /api/health` → `{"status":"healthy","database":"connected",...}`. Repetir 1–2 veces si el servicio estaba dormido (plan free en Render tarda ~35 s en despertar y el primer request puede dar un 500 transitorio, que se resuelve al reintentar).
2. **Base de datos real (Supabase):** `GET /api/tipos-evento` y `GET /api/eventos` (con token) devuelven datos reales (solo posibles si la pooler de Supabase responde).
3. **CORS hacia el frontend:** preflight `OPTIONS` con `Origin: https://planificador-eventos-frontend-ten.vercel.app` → cabecera `access-control-allow-origin` correcta.
4. **Frontend apunta al backend correcto:** el bundle JS del SPA desplegado contiene `planificador-eventos-backend-1.onrender.com`.

Más detalles de la validación en `boveda/mejoras/2026-09-24-014-validacion-conexion-front-back-db.md`.

## Estado y hoja de ruta

**Hecho**

- Infraestructura: capas Spring, conexión a Supabase (vía directa y pooler), CORS, Docker + `render.yaml`
- Endpoint `/api/health` con verificación real de la base
- Modelo de datos: entidades JPA, DTOs y DDL para Supabase, verificado contra PostgreSQL real
- Repositorios JPA con consultas por usuario, evento, fecha y estado
- Servicios de eventos y subtareas con validación, mapeo de relaciones y control de límite diario
- Catálogo de tipos de evento y actualización completa de subtareas
- API REST de eventos y subtareas con validación, cascada y manejo global de errores
- Endpoint `/api/subtareas/hoy` para consultar las gestiones no ejecutadas por fecha
- US-11: autenticación JWT stateless (registro, login, perfil), Swagger con `bearerAuth` y aislamiento real por propietario en eventos y subtareas

**Pendiente**

- Consolidar los usuarios existentes de Supabase con `password_hash` válido (hoy se resuelven registrando una cuenta nueva)
- Subir `PROTECT_SUBTAREAS=true` en Render una vez el frontend esté desplegado con el token

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