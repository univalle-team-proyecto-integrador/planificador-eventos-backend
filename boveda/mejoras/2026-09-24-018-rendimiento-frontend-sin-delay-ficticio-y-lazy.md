---
tipo: mejora
---
# Rendimiento del frontend: sin delay ficticio, timeout + reintento y lazy-loading

- **Fecha:** 2026-09-24
- **Área:** frontend / rendimiento
- **Estado:** hecha

## Descripción

El frontend desplegado se percibía lento al cargar/renderizar, aunque el backend en caliente responde en 0.3–0.6 s y la cadena es 5/5. Tras revisar el código del frontend (repo `planificador-eventos-frontend`) se encontró que la lentitud era del cliente: un **delay artificial de 600 ms** en todas las rutas (`SimulatedLoader`), fases de carga duplicadas, `fetch` sin timeout ni reintento (se colgaba ante el cold start de Render free) y un bundle único de ~301 kB sin división de código. Cambios aplicados directamente en el frontend, sin tocar el backend.

## Cambios (repo frontend)

- `src/routes/AppRoutes.jsx` — se eliminó `withLoader`/`SimulatedLoader` (delay ficticio de 600 ms en cada ruta) y se migró a **`React.lazy` + `Suspense`**: cada página es un chunk propio; el fallback es un spinner real sin temporizador. Las vistas ya tienen su propio estado de carga.
- `src/components/states/SimulatedLoader.jsx` — eliminado (sin referencias).
- `src/services/api.js` — `fetch` con **timeout de 15 s** (`AbortController`) y **reintento automático único** para lecturas (`GET`), pensado para el cold start de Render free; las escrituras no reintentan para evitar operaciones duplicadas. Mensajes de error diferenciados (timeout vs red).

## Verificación

- `npm run lint` (oxlint) → sin errores.
- `npm run build` (Vite 8) → OK en 432 ms. Bundle principal **301 kB → 263 kB** (gzip 84 kB); chunks por página: `DetallePage` 22 kB, `CrearPage` 7,4 kB, `ProgresoPage` 3,2 kB, `HoyPage` 0,8 kB, `LoginPage` 0,7 kB.
- N+1 de `ProgresoPage` ya operaba en paralelo (`Promise.all`); queda pendiente evaluar un endpoint del backend con el resumen de subtareas para reducirlo (requiere tocar el API).
- `bash scripts/verificar-despliegue.sh` → pendiente hasta redesplegar el frontend en Vercel.

## Pendientes

- Redesplegar el frontend (Vercel) para medir la ganancia real en producción y re-verificar la cadena con `scripts/verificar-despliegue.sh`.
- Evaluar (opcional) un endpoint del backend que incluya el resumen de progreso de subtareas por evento para eliminar el N+1 de la vista Progreso.