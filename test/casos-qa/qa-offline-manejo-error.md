---
tipo: caso-qa
area: frontend-e2e
estado: PASSED
fecha: 2026-09-25
---

# QA-303: Manejo de Error: Modo Offline

**ID Caso:** #303
**Tarea Relacionada:** Manejo de Error: Modo Offline
**Descripción / Escenario:** Verificar comportamiento cuando la API no responde.
**Pasos a Reproducir:**
1. Abrir la app desplegada (Vercel) apuntando al backend de Render.
2. En DevTools (F12) → pestaña **Network** → en el dropdown de *Throttling* activar **Offline**.
3. Intentar crear/guardar una tarea (crear evento en `/crear` o añadir gestión en el detalle `/evento/{id}`).
4. Verificar toast/alert de error de conexión; no debe crearse nada.
5. Volver online (desactivar Offline) y reintentar la acción; entonces debe guardarse.

**Resultado Esperado:** Al guardar estando en Offline, la app muestra toast/alert de error de conexión y no crea nada; el botón "Reintentar" reintenta la acción (o permite reintentar tras volver online); la UI no se bloquea.

**Resultado Obtenido:** Se recibe una retroalimentación de la página haciendo saber al usuario que hubo un error al crear la actividad y no cambia de página.

**Estado:** PASSED

---

**Notas técnicas (evidencia interna):**
- `src/views/CreateEventView.jsx` (`submitEvent`, catch en línea 166): muestra toast `notifyError` con "No pudimos guardar el evento" y **no navega**; el canvas sigue en `/crear` y el botón vuelve a habilitarse (`finally` de `setIsSubmitting(false)`). Nada se persiste porque el `POST` de `api.js` falla antes de tocar el servidor.
- `src/views/EventDetailView.jsx` (añadir gestión, catch en línea 355): idem con "No pudimos añadir la gestión"; el listado no se modifica.
- **No hay reintento automático en POST** (decisión D-007 "Actualizaciones confirmadas por el servidor", en `docs/decisiones-ux.md`): al volver online el usuario reintenta pulsando de nuevo "Guardar". Solo los `GET` reintentan una vez (cold start de Render free), implementado en `src/services/api.js`.
- El "Reintentar" visible aplica a la carga del catálogo de tipos de evento (`CreateEventView.jsx:312`), no a la creación en sí.