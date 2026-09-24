---
tipo: mejora
---

# Interfaz frontend de creación, detalle y estados visuales

- **Fecha:** 2026-09-24
- **Área:** frontend / UX / integración API
- **Estado:** hecha

## Descripción

Se reorganizó el frontend alrededor de un `Layout` persistente y un enrutador SPA centralizado. Se implementaron las pantallas de creación y detalle de eventos, el sistema de estados visuales, validaciones accesibles y la capa de integración HTTP usada por US-01, US-02 y US-03.

El alcance de esta nota corresponde al repositorio `planificador-eventos-frontend`. La persistencia real queda dependiente de que el backend exponga los endpoints de eventos y subtareas con el contrato documentado.

## Cambios

- `src/routes/AppRoutes.jsx` — rutas `/hoy`, `/crear`, `/evento/:id`, `/progreso`, `/login` y redirección de rutas desconocidas.
- `src/components/ui/Layout.jsx` — encabezado global, navegación, enlace de salto al contenido y `Outlet`.
- `src/components/ui/Button.jsx` — botón multivariante (`primary`, `neutral`, `danger`) con soporte para renderizarse como `Link` y propagar atributos de accesibilidad.
- `src/components/states/` — `EmptyState`, `ErrorState` y `SimulatedLoader` conectados a los flujos principales.
- `src/components/ui/ConfirmModal.jsx` — confirmación de eliminación con `Escape`, focus trap y restauración del foco.
- `src/components/ui/ProgressBar.jsx` — indicador accesible del avance logístico.
- `src/components/ui/EventCard.jsx` y `src/pages/ProgresoPage.jsx` — tarjetas de eventos, consulta de subtareas y progreso global.
- `src/views/CreateEventView.jsx` — formulario US-01, validaciones con la regla “qué pasó + cómo corregirlo”, estado de envío y manejo de error.
- `src/views/EventDetailView.jsx` — lectura y edición del evento, subtareas dinámicas, progreso, completar/reabrir y eliminación segura.
- `src/services/api.js` — cliente HTTP centralizado con `VITE_API_URL`, `VITE_USER_ID` y mapeo de fechas.
- `docs/decisiones-ux.md`, `docs/guia-microcopy.md` y `docs/auditoria-a11y.md` — decisiones de UX, accesibilidad, estados, estándar de mensajes y checklist de auditoría.
- `.env.example` y `vercel.json` — configuración local de API y fallback de React Router en Vercel.
- `README.md` y `AGENTS.md` — arquitectura, rutas, contrato de API y convenciones actualizadas.

## Contrato esperado del backend

- `GET /api/eventos?usuarioId={id}` — listar eventos para el panel de progreso.
- `POST /api/eventos` — crear un evento.
- `GET /api/eventos/{id}` — consultar un evento.
- `PUT /api/eventos/{id}` — actualizar un evento.
- `GET /api/subtareas?eventoId={id}` — consultar subtareas.
- `POST /api/subtareas` — crear una subtarea.
- `PUT /api/subtareas/{id}` — cambiar su estado o datos.
- `DELETE /api/subtareas/{id}` — eliminar una subtarea.

Los DTO utilizados por el frontend son `EventoDTO` y `SubtareaDTO` del backend. La interfaz muestra `ErrorState` cuando el servidor no está disponible; no se ocultan errores de integración mediante datos simulados.

## Verificación

- `npm run lint` — 0 errores y 0 advertencias.
- `npm run build` — build de producción generado correctamente en `dist/`.
- `npx prettier --check ...` — código y documentación formateados.
- `git diff --check` — sin espacios finales ni conflictos de whitespace.
- Revisión de código de navegación, formularios, estados y focus trap; la prueba con lector de pantalla queda para el entorno de despliegue.
- Prueba end-to-end contra el backend — pendiente de publicar y desplegar la implementación local de los controladores; el cliente ya está alineado con el contrato de la mejora 010.

## Pendiente de integración

- Publicar en el backend los endpoints REST indicados arriba y revisarlos en `backend/lead`; la implementación local queda descrita en `2026-09-24-010-api-rest-eventos-subtareas.md`.
- Confirmar que el servidor acepta `idUsuario`, `idTipoEvento` y `LocalDateTime` con el formato generado por el cliente.
- Sustituir `VITE_USER_ID` temporal por autenticación cuando la US-11 esté disponible.
- Ejecutar la prueba end-to-end con la API real y registrar el resultado en esta nota.
