---
tipo: mejora
---

# CORS para 127.0.0.1 y ProblemDetail en errores de parámetros y métodos

- **Fecha:** 2026-09-29
- **Área:** API / configuración
- **Estado:** hecha

## Descripción

Dos defectos encontrados al revisar la cadena frontend → backend. El primero rompía el trabajo local: `app.cors.allowed-origins` solo listaba `http://localhost:5173`, así que abrir el frontend con `http://127.0.0.1:5173` (lo que hacen algunos navegadores y clientes HTTP al normalizar `localhost`) producía un preflight con 403. El segundo era una inconsistencia del contrato de errores: `GlobalExceptionHandler` solo interceptaba las excepciones de negocio y de validación del cuerpo, por lo que un `@RequestParam` ausente devolvía 400 **con el cuerpo vacío**, sin el `ProblemDetail` que sí recibían el resto de errores, y un método HTTP no soportado devolvía un 405 genérico de Spring.

## Cambios

- `src/main/resources/application.properties` — se añadió `http://127.0.0.1:5173` a `app.cors.allowed-origins`, con un comentario que explica por qué.
- `src/main/java/uv/isj/planificadoreventosbackend/exception/GlobalExceptionHandler.java` — se añadieron tres manejadores:
  - `MissingServletRequestParameterException` → 400, título `Solicitud inválida`, detalle `Falta el parámetro obligatorio '<nombre>'` y `errors.<nombre>`.
  - `MethodArgumentTypeMismatchException` → 400, título `Solicitud inválida`, detalle `El valor '<valor>' no es válido para el parámetro '<nombre>'` y `errors.<nombre>`. Cubre a la vez los `id` de ruta no numéricos y las fechas con formato inválido.
  - `HttpRequestMethodNotSupportedException` → 405, título `Método no permitido`, detalle con el método recibido y los permitidos, más la cabecera `Allow` que exige RFC 9110.
- `src/test/java/uv/isj/planificadoreventosbackend/config/CorsConfigTest.java` — `127.0.0.1:5173` pasa a la lista de orígenes aceptados; se agregan rechazos para `127.0.0.1:5174`, `https://localhost` y `https://localhost:5173`.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ParametrosApiTest.java` — se reescribieron las aserciones para exigir `application/problem+json`, título, detalle y `errors`, además de la cabecera `Allow` en los 405.
- `boveda/mejoras/README.md` y `boveda/lienzo-maestro.canvas` — se registró la mejora 023.

## Nota sobre las pruebas de CORS

El caso `http://localhost` (sin puerto) no puede probarse con MockMvc: la petición por defecto ya es `http://localhost:80`, así que Spring la considera *same-origin* y se salta el procesamiento CORS, devolviendo 200 por la razón equivocada. Se documentó en el test y se cubrió en su lugar con `https://localhost`.

## Verificación

- `./mvnw test` → **BUILD SUCCESS**, 109 pruebas, 0 fallos y 0 errores.
- Contra el servidor local (`./mvnw spring-boot:run`, puerto 8080, base de datos conectada):
  - Preflight: 200 con `Access-Control-Allow-Origin` para `http://localhost:5173`, `http://127.0.0.1:5173`, `https://planificador-eventos-frontend-ten.vercel.app` y `https://otro.vercel.app`; 403 sin ACAO para `http://localhost:5174` y `https://sitio-malicioso.com`.
  - `GET /api/subtareas` → 400 `{"detail":"Falta el parámetro obligatorio 'eventoId'","status":400,"title":"Solicitud inválida","errors":{"eventoId":"parámetro obligatorio"}}` con `Content-Type: application/problem+json`.
  - `GET /api/subtareas?eventoId=abc` → 400 con el detalle del valor inválido.
  - `POST /api/health` → 405 con `Allow: GET` y `{"title":"Método no permitido","status":405}`.
