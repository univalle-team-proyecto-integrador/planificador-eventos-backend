---
tipo: mejora
---

# Validación de la conexión frontend → backend → Supabase

- **Fecha:** 2026-09-24
- **Área:** infraestructura / integración
- **Estado:** hecha

## Descripción

Se trazó y ejecutó una ruta de verificación de la cadena completa entre el frontend desplegado en Vercel, el backend Spring Boot en Render y la base PostgreSQL en Supabase. Además se creó una ruta reutilizable (`scripts/verificar-despliegue.sh`) para repetir la validación después de cualquier cambio o redeploy, y se corrigió la documentación que apuntaba a URLs públicas equivocadas.

### URLs reales del despliegue (verificadas)

- Frontend (Vercel): `https://planificador-eventos-frontend-ten.vercel.app` — SPA Vite.
- Backend (Render, Spring Boot): `https://planificador-eventos-backend-1.onrender.com`.
- Base (Supabase): pooler transaccional `aws-0-us-west-2.pooler.supabase.com:6543` (`postgres.akyvplcsiqhmfkqxvgoq`).

## Evidencia del sondeo

1. **Frontend vivo:** `curl https://planificador-eventos-frontend-ten.vercel.app` → HTTP 200, build Vite (`/assets/index-*.js`).
2. **Bundle apunta al backend correcto:** el JS desplegado contiene `planificador-eventos-backend-1.onrender.com`.
3. **Backend Spring vivo:** `GET /api/health` → `{"status":"healthy","database":"connected"}`.
4. **OpenAPI:** `GET /v3/api-docs` → HTTP 200; Swagger UI disponible.
5. **Base real:** `GET /api/tipos-evento` → catálogo de Supabase (Boda, Corporativo, Cumpleaños, Otro, Social); `GET /api/eventos?usuarioId=1` → datos reales del seed.
6. **CORS:** preflight `OPTIONS` y `GET` con `Origin: https://planificador-eventos-frontend-ten.vercel.app` devuelven `access-control-allow-origin` correcto con `allow-credentials: true`.

## Hallazgos

- El dominio `https://planificador-eventos-backend.onrender.com` (sin sufijo `-1`) sigue sirviendo otra API (no Spring): `/api/health` → `{"status":"ok","message":"API funcionando correctamente"}` y `/v3/api-docs` → 404. Es un servicio heredado/ajeno; nunca tratarlo como este backend.
- `https://planificador-eventos-frontend.vercel.app` (sin sufijo `-ten`) sirve un build de Create React App que apunta a `backend.vercel.app`/... — tampoco es este proyecto.
- Render plan free duerme: el primer request tras despertar tardó ~35 s y un `GET` devolvió un 500 transitorio (200 al reintentar). La verificación debe reintentar al menos una vez.

## Cambios

- `README.md` — URLs públicas de Swagger/OpenAPI y de despliegue apuntan a `-1`; nota de estado reescrita (Spring desplegado y verificado; `-0` es ajeno); nueva sección «Ruta para verificar la conexión tras un despliegue».
- `AGENTS.md` — service URL corregida a `-1`, nota del dominio heredado, frontend de referencia y recordatorio de ejecutar `scripts/verificar-despliegue.sh` tras cada redeploy.
- `scripts/verificar-despliegue.sh` — script de humo reutilizable (nuevo).
- `boveda/mejoras/README.md` — fila 014 añadida al índice.
- `boveda/lienzo-maestro.canvas` — nodo enlazado a esta mejora.

## Verificación

- Ejecución real de la ruta completa (puntos 1–6 de «Evidencia del sondeo») contra los despliegues de Vercel y Render.
- `bash scripts/verificar-despliegue.sh` — todos los pasos en verde.
- `git diff --check` — sin errores de formato.