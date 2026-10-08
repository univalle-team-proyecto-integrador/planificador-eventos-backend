# Pruebas del backend con Postman

Pruebas de la API (desplegada en Render) usando **Postman**, como se exige para validar el backend. Se apoyan en la spec OpenAPI (`/v3/api-docs`), que Postman importa y convierte en colección automáticamente.

> Postman no es un navegador, así que **no interviene CORS** en estas pruebas. El frontend sí pasa CORS (ver [conexion.md](conexion.md)).

## 1. Opción rápida (recomendada): importar la colección ya lista

La carpeta [`test/postman/`](postman/) trae la colección y el entorno ya preparados:

1. Postman → **Import** → arrastrar `planificador-eventos-backend.postman_collection.json`.
2. Postman → **Import** → `planificador-eventos-backend.postman_environment.json` (aparece en *Environments*).
3. A la derecha arriba, seleccionar el entorno **Planificador de Eventos — Render**.

La colección usa variables: `{{baseUrl}}`, `{{usuarioId}}`, `{{idEvento}}`, `{{idSubtarea}}`, `{{idTipoEvento}}`. Tras crear un evento, copiar su `idEvento` en el entorno para el resto de llamadas.

## 2. Opción automática: importar la spec desde la URL

Postman → **Import → Link** → pegar `https://planificador-eventos-backend-1.onrender.com/v3/api-docs`. Postman genera la colección con todos los endpoints a partir de la spec (que ya está verificada en el despliegue).

## 3. Secuencia de pruebas (orden sugerido)

| # | Petición | Método y ruta | Esperado |
| --- | --- | --- | --- |
| 1 | Health | `GET {{baseUrl}}/api/health` | 200 `{"status":"healthy","database":"connected",...}` |
| 2 | Tipos de evento | `GET {{baseUrl}}/api/tipos-evento` | 200 catálogo |
| 3 | Eventos del usuario | `GET {{baseUrl}}/api/eventos?usuarioId={{usuarioId}}` | 200 |
| 4 | **Crear evento** | `POST {{baseUrl}}/api/eventos` | 201 con `idEvento` |
| 5 | Detalle | `GET {{baseUrl}}/api/eventos/{{idEvento}}` | 200 |
| 6 | Plan logístico | `GET {{baseUrl}}/api/eventos/{{idEvento}}/subtareas` | 200 |
| 7 | Actualizar evento | `PUT {{baseUrl}}/api/eventos/{{idEvento}}` | 200 |
| 8 | **Añadir subtarea** | `POST {{baseUrl}}/api/eventos/{{idEvento}}/subtareas` | 201 con `idSubtarea` |
| 9 | Subtarea por evento | `GET {{baseUrl}}/api/subtareas?eventoId={{idEvento}}` | 200 |
| 10 | Cambiar estado | `PATCH {{baseUrl}}/api/subtareas/{{idSubtarea}}/estado` | 200 (`ejecutada`) |
| 11 | Reprogramar | `PATCH {{baseUrl}}/api/subtareas/{{idSubtarea}}/reprogramar` | 200 |
| 12 | Editar subtarea | `PUT {{baseUrl}}/api/subtareas/{{idSubtarea}}` | 200 |
| 13 | Eliminar subtarea | `DELETE {{baseUrl}}/api/subtareas/{{idSubtarea}}` | 204 |
| 14 | Eliminar evento | `DELETE {{baseUrl}}/api/eventos/{{idEvento}}` | 204 (limpieza) |
| 15 | Error de validación | `POST {{baseUrl}}/api/eventos` sin `nombre` | 400 |
| 16 | No encontrado | `GET {{baseUrl}}/api/eventos/999999` | 404 |

Norma: crear y eliminar (limpieza) para no ensuciar Supabase; verificar al final con `GET /api/eventos?usuarioId=1`.

## 4. Evidencia para entregar

- Capturas de Postman con **código de estado + respuesta JSON** (mínimo: Health, crear evento 201, subtarea 201, estado 200, eliminar 204, validación 400).
- Opcional: *Export Collection* para revisión, o guardar el reporte de runner.

## 5. Notas

- La app vive en el subdominio `...-1.onrender.com` (Spring). El `...-0.onrender.com` es otra API y **no** debe probarse (ver [conexion.md](conexion.md)).
- Si Render está dormido (free), el primer request puede tardar ~35 s y devolver 500 transitorio; reintentar una vez.
- La colección parte de `{{usuarioId}}=1`, el usuario de prueba real.