---
tipo: mejora
---

# Pruebas de CORS, salud, parámetros y OpenAPI, y fix del verificador de despliegue

- **Fecha:** 2026-09-29
- **Área:** pruebas / infraestructura
- **Estado:** hecha

## Descripción

La revisión de la cadena frontend → backend → Supabase dejó varias zonas sin cobertura automática: la política CORS (que es la causa más común de un fallo visible en el navegador), la rama 503 de `/api/health`, el contrato HTTP de los parámetros de consulta, el endpoint `GET /api/subtareas/{id}` y la fecha predeterminada de la vista Hoy. Además, `scripts/verificar-despliegue.sh` reportaba un falso negativo: solo revisaba el primer `<script>` del HTML, pero Vite mueve el cliente HTTP a un chunk *lazy* (`assets/api-*.js`) que no aparece ahí, así que la comprobación fallaba aunque el despliegue fuera correcto.

## Cambios

- `src/test/java/uv/isj/planificadoreventosbackend/config/CorsConfigTest.java` (nuevo) — 11 pruebas: preflight aceptado para `localhost:5173` y cualquier subdominio `*.vercel.app`; preflight rechazado (403) para `127.0.0.1:5173`, `localhost:5174`, `https://sitio-malicioso.com` y el patrón `vercel.app.otro-dominio.com`; métodos `GET/POST/PUT/PATCH/DELETE/OPTIONS`; `Allow-Credentials: true`; petición real con origen permitido y bloqueada (403); y que la política solo aplica a `/api/**` (`/v3/api-docs` no recibe cabeceras CORS).
- `src/test/java/uv/isj/planificadoreventosbackend/controller/HealthControllerBaseDatosCaidaTest.java` (nuevo) — con `@MockitoBean HealthService` se cubre el 503 `unhealthy/disconnected` en `/api/health` y `/api/health/`.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ParametrosApiTest.java` (nuevo) — parámetros requeridos ausentes, `eventoId` no numérico, fecha inválida, `id` de ruta no numérico, estado de subtarea inexistente, métodos no soportados (405) y rutas inexistentes (404).
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ApiSprint1Test.java` — se cubre `GET /api/subtareas/{id}` con todos los campos del DTO.
- `src/test/java/uv/isj/planificadoreventosbackend/service/SubtareaServiceTest.java` — se cubren `obtenerPorId` (mapeo y 404), la fecha predeterminada `America/Bogota` cuando no se envía `fecha` (con `ArgumentCaptor` y tolerancia de un día), el respeto de la fecha enviada y el rechazo de `usuarioId` nulo o cero.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/SwaggerOpenApiTest.java` — se añade la ruta `/api/subtareas/hoy`, una aserción global de que **ninguna** respuesta documentada usa `*/*`, un recorrido de todas las operaciones para exigir `application/json` en cada respuesta con cuerpo, la documentación de parámetros y `requestBody`, los códigos de respuesta (incluido el 503 de health) y los esquemas `HealthResponse`, `EventoDTO`, `SubtareaDTO` y `ReprogramarDTO`.
- `scripts/verificar-despliegue.sh` — el paso 4 ahora recorre el grafo de imports desde el entrypoint (con `normalizar`/`resolver` en bash puro, sin `python3`), con límite de 40 bundles, e informa en qué chunk se encontró la URL del backend.
- `boveda/mejoras/README.md` y `boveda/lienzo-maestro.canvas` — se registró la mejora 022.

## Hallazgos

- Un origen no permitido devuelve **403 "Invalid CORS request"** también en peticiones reales, no solo en preflight. El navegador lo reporta como error de red, así que conviene incluirlo en el diagnóstico del frontend.
- `http://127.0.0.1:5173` **no** está permitido: la propiedad `app.cors.allowed-origins` solo lista `http://localhost:5173`. Quien abra el frontend con `127.0.0.1` verá fallos de CORS aunque todo lo demás esté bien.
- Cuando falta un `@RequestParam` obligatorio (`GET /api/subtareas` sin `eventoId`), Spring responde 400 **con el cuerpo vacío**: `GlobalExceptionHandler` no cubre `MissingServletRequestParameterException`, así que no llega el `ProblemDetail` que sí devuelve el resto de errores. Queda anotado como pendiente; no se modificó código de producción en esta mejora.

## Verificación

- `./mvnw test` → **BUILD SUCCESS**, 103 pruebas, 0 fallos y 0 errores (antes 70).
- `bash scripts/verificar-despliegue.sh` → **5 correctos, 0 fallidos**; detecta la URL en `/assets/api-DG8p8SxS.js` tras revisar 13 bundles.
- Caso negativo del script (URL de backend falsa) → correctamente reporta `[FAIL] ningún bundle de 13 revisados referencia ...`.
- `python3 -m json.tool boveda/lienzo-maestro.canvas` → JSON válido.
