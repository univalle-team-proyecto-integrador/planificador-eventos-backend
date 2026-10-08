# Pruebas del frontend en Chrome

Pruebas de la SPA desplegada (Vercel) con el navegador **Google Chrome**, como se exige para validar el frontend. Se recorre la app de punta a punta y se confirma en la pestaña **Network** que cada acción llama al backend de Render (`/api/**`) con 200.

> URL desplegada: `https://planificador-eventos-frontend-ten.vercel.app` (Vite). El `planificador-eventos-frontend.vercel.app` (sin `-ten`) es otro build — no es este proyecto (ver [conexion.md](conexion.md)).

## Rutas de la app (verificadas en `routes/AppRoutes.jsx`)

| Ruta | Vista |
| --- | --- |
| `/` | Redirige a `/hoy` |
| `/hoy` | Panel de hoy (lista de eventos) |
| `/crear` | Formulario de creación de evento |
| `/evento/:id` | Detalle del evento + plan logístico (subtareas) |
| `/progreso` | Progreso (resumen) |
| `/login` | Pantalla placeholder (acceso en próxima iteración, sin backend) |

## Paso a paso en Chrome

1. Abrir `https://planificador-eventos-frontend-ten.vercel.app` en Chrome.
2. **F12 → pestaña Network** → filtro `Fetch/XHR`.
3. Recorrer los flujos del checklist y confirmar en cada acción que aparece la llamada a `https://planificador-eventos-backend-1.onrender.com/api/**` con status 200 (y el JSON de respuesta en la pestaña *Payload/Response* o *Preview*).
4. Recargar (`F5`) al entrar para capturar el `GET /api/eventos?usuarioId=1` inicial.

## Checklist de flujos (marca ✓ / ✗)

| # | Flujo | Acción | Llamada esperada |
| --- | --- | --- | --- |
| 1 | Carga inicial | Entrar a `/` (→ `/hoy`) | `GET /api/eventos?usuarioId=1` → 200 |
| 2 | Crear evento | `/crear`: llenar formulario y guardar | `POST /api/eventos` → 201 |
| 3 | Detalle | Abrir el evento creado desde el panel | `GET /api/eventos/{id}` → 200; `GET /api/eventos/{id}/subtareas` → 200 |
| 4 | Editar evento | Modificar datos y guardar | `PUT /api/eventos/{id}` → 200 |
| 5 | Añadir subtarea | En el detalle, crear una subtarea | `POST /api/eventos/{id}/subtareas` → 201 |
| 6 | Reprogramar subtarea | Cambiar fecha/horas de una subtarea | `PATCH /api/subtareas/{id}/reprogramar` → 200 |
| 7 | Cambiar estado | Marcar subtarea como ejecutada/pospuesta | `PATCH /api/subtareas/{id}/estado` → 200 |
| 8 | Editar subtarea | Modificar nombre/fecha de una subtarea | `PUT /api/subtareas/{id}` → 200 |
| 9 | Eliminar subtarea | Borrar la subtarea | `DELETE /api/subtareas/{id}` → 204 |
| 10 | Eliminar evento | Borrar el evento de prueba (limpieza) | `DELETE /api/eventos/{id}` → 204 |
| 11 | Progreso | Abrir `/progreso` | `GET`(s) → 200 |
| 12 | Estados de UI | Lista vacía (sin eventos) y error (backend dormido) | comportamiento visual + reintento |

**Norma:** crear y eliminar para no ensuciar Supabase; al final `GET /api/eventos?usuarioId=1` devuelve la lista original.

## Evidencia para entregar

- Screenshots de Chrome con la app en cada flujo clave (crear, detalle con subtareas, progreso, eliminar).
- Screenshots de la pestaña **Network** mostrando la llamada real a `...-1.onrender.com/api/...` y su status 200/204.

## Notas

- **CORS**: las llamadas desde `https://planificador-eventos-frontend-ten.vercel.app` pasan el control de `config/CorsConfig.java` (`allowedOriginPatterns=https://*.vercel.app`). Si al probar en `localhost:5173` contra el backend desplegado también pasa, es el patrón correcto para dev local.
- **Cold start (Render free)**: si el backend duerme, el primer `GET` del frontend tardará ~35 s y puede fallar una vez; recargar. El frontend debe mostrar el estado de error y permitir reintentar.
- El `Login` (ruta `/login`) es **placeholder**: no hay petición de autenticación (el acceso llega en la siguiente iteración). No es un fallo.
- Los ids de prueba son los de Supabase real (el frontend usa `usuarioId=1`). Alternativamente probar local siguiendo [pruebas-local.md](pruebas-local.md).