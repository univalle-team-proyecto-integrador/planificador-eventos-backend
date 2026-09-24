# Documentación de la API con Swagger

La API se documenta y se prueba en Swagger UI (springdoc-openapi). Es la **documentación oficial** del backend y la evidencia solicitada es una captura con un endpoint expandido y su respuesta de *Try it out*.

## URLs

| Recurso | URL |
| --- | --- |
| Swagger UI | `https://planificador-eventos-backend-1.onrender.com/swagger-ui.html` (redirige a `/swagger-ui/index.html`) |
| Spec OpenAPI (JSON) | `https://planificador-eventos-backend-1.onrender.com/v3/api-docs` |
| Spec OpenAPI (YAML) | `https://planificador-eventos-backend-1.onrender.com/v3/api-docs.yaml` |

Verificado en el despliegue (2026-09-24): `/swagger-ui.html` responde 302 → `/swagger-ui/index.html`; `/v3/api-docs` responde 200 con `"servers":[{"url":"/","description":"Servidor actual"}]`.

## El rol de Swagger (no es un puente)

Swagger **no está entre** el frontend y el backend:

- El SPA (Vercel) llama **directo** a `/api/**` del backend; nunca pasa por Swagger.
- Swagger es una UI que sirve **el propio backend** en su misma URL y sirve para que una persona documente y pruebe la API a mano, en **paralelo** al frontend, no como intermediario.
- Al abrir *Try it out*, las peticiones salen del navegador hacia el **mismo host** (servidor relativo `/`), por lo que no hay CORS (mismo origen). Se comporta igual que un cliente cualquiera llamando a `/api/**`.

```
Frontend (Vercel) ──HTTPS──▶ /api/**         ──▶ Controllers ▶ Services ▶ Supabase
Swagger UI (mismo host) ────▶ /api/** (Try it out, mismo origen)
```

## Cómo documentar/probar con Swagger

1. Abrir `https://planificador-eventos-backend-1.onrender.com/swagger-ui.html`.
2. Expandir el endpoint deseado (ej. `GET /api/eventos` o `POST /api/eventos`).
3. En *Try it out* rellenar parámetros con el usuario real (`usuarioId=1`) o el cuerpo JSON del ejemplo.
4. *Execute* → revisar código de respuesta (200/201/400/404) y el cuerpo JSON.
5. **Evidencia**: captura del endpoint expandido + respuesta del *Try it out*.

## Endpoints expuestos

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/health` | Estado de la app y de la BD |
| GET | `/api/tipos-evento` | Catálogo de tipos de evento |
| GET | `/api/eventos?usuarioId=1` | Eventos del organizador |
| GET | `/api/eventos/{id}` | Detalle de un evento |
| GET | `/api/eventos/{id}/subtareas` | Plan logístico del evento |
| POST | `/api/eventos` | Crear evento |
| PUT | `/api/eventos/{id}` | Actualizar evento |
| DELETE | `/api/eventos/{id}` | Eliminar evento |
| POST | `/api/eventos/{id}/subtareas` | Añadir subtarea |
| GET | `/api/subtareas?eventoId={id}` | Subtareas por evento |
| GET | `/api/subtareas/{id}` | Detalle de una subtarea |
| PUT | `/api/subtareas/{id}` | Editar una subtarea |
| PATCH | `/api/subtareas/{id}/estado` | Cambiar estado (`pendiente`/`ejecutada`/`pospuesta`) |
| PATCH | `/api/subtareas/{id}/reprogramar` | Reprogramar fecha y horas |
| DELETE | `/api/subtareas/{id}` | Eliminar una subtarea |

## Cuerpos de ejemplo (los mismos que usa Postman)

`POST /api/eventos`:

```json
{
  "idUsuario": 1,
  "idTipoEvento": 1,
  "nombre": "Evento de prueba Swagger",
  "cliente": "Equipo QA",
  "fechaEvento": "2026-12-05T10:00:00",
  "lugar": "Salón de pruebas"
}
```

`PATCH /api/subtareas/{id}/estado`:

```json
{
  "estado": "ejecutada",
  "notaExplicativa": "Verificado por QA"
}
```

Ver la [colección Postman](postman/) para el resto de cuerpos; son los mismo DTOs (`model/dto/`).