# Pruebas del backend con Postman

Pruebas de la API (desplegada en Render) usando **Postman**, como se exige para validar el backend. Se apoyan en la spec OpenAPI (`/v3/api-docs`), que Postman importa y convierte en colección automáticamente.

> Postman no es un navegador, así que **no interviene CORS** en estas pruebas. El frontend sí pasa CORS (ver [conexion.md](conexion.md)).

## 1. Opción rápida (recomendada): importar la colección ya lista

La carpeta [`test/postman/`](postman/) trae la colección y el entorno ya preparados:

1. Postman → **Import** → arrastrar `planificador-eventos-backend.postman_collection.json`.
2. Postman → **Import** → `planificador-eventos-backend.postman_environment.json` (aparece en *Environments*).
3. A la derecha arriba, seleccionar el entorno **Planificador de Eventos — Render**.

La colección usa variables: `{{baseUrl}}`, `{{token}}`, `{{email}}`, `{{password}}`, `{{nombre}}`, `{{usuarioId}}`, `{{idEvento}}`, `{{idSubtarea}}`, `{{idTipoEvento}}`, `{{fechaHoy}}`. Incluye el grupo **0. Autenticación (US-11)**, cuyos `POST /api/users/register` y `POST /api/users/login` guardan el `{{token}}` en una variable de colección; el resto de peticiones lo envían como `Authorization: Bearer {{token}}`. `{{usuarioId}}` se ignora (el propietario sale del token). Tras crear un evento, copiar su `idEvento` en el entorno para el resto de llamadas.

## 2. Opción automática: importar la spec desde la URL

Postman → **Import → Link** → pegar `https://planificador-eventos-backend-1.onrender.com/v3/api-docs`. Postman genera la colección con todos los endpoints a partir de la spec (que ya está verificada en el despliegue).

## 3. Secuencia de pruebas (orden sugerido)

| # | Petición | Método y ruta | Esperado |
| --- | --- | --- | --- |
| 1 | Registro | `POST {{baseUrl}}/api/users/register` | 201 con `token` (lo guarda en `{{token}}`) |
| 2 | Login | `POST {{baseUrl}}/api/users/login` | 200 con `token` |
| 3 | Perfil | `GET {{baseUrl}}/api/users/profile` | 200 (usuario del token) |
| 4 | Health | `GET {{baseUrl}}/api/health` | 200 `{"status":"healthy","database":"connected",...}` |
| 5 | Tipos de evento | `GET {{baseUrl}}/api/tipos-evento` | 200 catálogo |
| 6 | Eventos del usuario | `GET {{baseUrl}}/api/eventos?usuarioId={{usuarioId}}` | 200 (`usuarioId` se ignora) |
| 7 | **Crear evento** | `POST {{baseUrl}}/api/eventos` | 201 con `idEvento` |
| 8 | Detalle | `GET {{baseUrl}}/api/eventos/{{idEvento}}` | 200 |
| 9 | Plan logístico | `GET {{baseUrl}}/api/eventos/{{idEvento}}/subtareas` | 200 |
| 10 | Actualizar evento | `PUT {{baseUrl}}/api/eventos/{{idEvento}}` | 200 |
| 11 | **Añadir subtarea** | `POST {{baseUrl}}/api/eventos/{{idEvento}}/subtareas` | 201 con `idSubtarea` |
| 12 | Subtarea por evento | `GET {{baseUrl}}/api/subtareas?eventoId={{idEvento}}` | 200 |
| 13 | Cambiar estado | `PATCH {{baseUrl}}/api/subtareas/{{idSubtarea}}/estado` | 200 (`ejecutada`) |
| 14 | Reprogramar | `PATCH {{baseUrl}}/api/subtareas/{{idSubtarea}}/reprogramar` | 200 |
| 15 | Editar subtarea | `PUT {{baseUrl}}/api/subtareas/{{idSubtarea}}` | 200 |
| 16 | Eliminar subtarea | `DELETE {{baseUrl}}/api/subtareas/{{idSubtarea}}` | 204 |
| 17 | Eliminar evento | `DELETE {{baseUrl}}/api/eventos/{{idEvento}}` | 204 (limpieza) |
| 18 | Error de validación | `POST {{baseUrl}}/api/eventos` sin `nombre` | 400 |
| 19 | No encontrado | `GET {{baseUrl}}/api/eventos/999999` | 404 |

Norma: crear y eliminar (limpieza) para no ensuciar Supabase; verificar al final con `GET /api/eventos` (el `usuarioId` del query se ignora: manda el token).

## 4. Evidencia para entregar

- Capturas de Postman con **código de estado + respuesta JSON** (mínimo: Health, crear evento 201, subtarea 201, estado 200, eliminar 204, validación 400).
- Opcional: *Export Collection* para revisión, o guardar el reporte de runner.

## 5. Notas

- La app vive en el subdominio `...-1.onrender.com` (Spring). El `...-0.onrender.com` es otra API y **no** debe probarse (ver [conexion.md](conexion.md)).
- Si Render está dormido (free), el primer request puede tardar ~35 s y devolver 500 transitorio; reintentar una vez.
- El `{{usuarioId}}=1` de la colección se ignora: el propietario sale del `{{token}}`; sin token, el backend lo honra solo por compatibilidad con el cliente legado.