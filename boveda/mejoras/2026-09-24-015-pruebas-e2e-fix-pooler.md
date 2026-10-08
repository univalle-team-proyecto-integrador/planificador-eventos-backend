---
tipo: mejora
---

# Pruebas e2e y fix del pooler: prepared statements

- **Fecha:** 2026-09-24
- **Área:** infraestructura / pruebas
- **Estado:** hecha

## Descripción

Como cierre de la validación de la conexión (mejora 014), se creó un folder `test/` con documentación descentralizada y clara sobre entornos, conexión, validación de despliegue y pruebas locales, y se ejecutaron las pruebas sobre el stack real. Durante el e2e local se descubrió y corrigió un bug real del pooler transaccional de Supabase.

## Cambios

- `test/` (nuevo, documentación descentralizada en español): `README.md` (mapa), `entorno.md`, `conexion.md`, `validacion-despliegue.md`, `pruebas-local.md`, `terminos-clave.md`, `plantilla-caso-prueba.md`.
- `planificador-eventos-frontend/.env` — reemplazado el template basura (APP_NAME/DB_HOST/JWT) por el real: `VITE_API_URL=http://localhost:8080`, `VITE_USER_ID=1`.
- `planificador-eventos-backend/.env` — confirmado correcto (pooler + `postgres.akyvplcsiqhmfkqxvgoq` + password real validada con `psql SELECT 1`).
- `src/main/resources/application.properties` — **fix**: `spring.datasource.hikari.data-source-properties.prepareThreshold=0` para el pooler transaccional (PgBouncer + pgjdbc).
- `boveda/mejoras/README.md` — fila 015.
- `boveda/lienzo-maestro.canvas` — nodo enlazado.

## Bug encontrado: `prepared statement "S_1" already exists`

- Al hacer POST/UPDATE contra Supabase vía pooler, intermitentemente 500 con `PSQLException: ERROR: prepared statement "S_1" already exists`.
- Causa: PgBouncer reutiliza sesiones de servidor; pgjdbc cachea prepared statements server-side (`S_1`, `S_2`...) que colisionan entre conexiones del pool.
- Fix: `prepareThreshold=0` (desactiva prepared statements server-side), recomendado por Supabase para el pooler transaccional.
- El despliegue de Render aún usa la imagen previa (funciona pero es flaky): **recrear el servicio con el blueprint** para incluir el fix.

## Verificación

- `psql SELECT 1` contra pooler → OK (credencial real validada).
- `./mvnw test` → 35 tests, 0 fallos, 0 errores (H2).
- E2E local contra Supabase real: `/api/health` healthy/connected → POST evento (201) → aparece en listado → DELETE (204) → confirmada limpieza (0 coincidencias).
- `npm run lint` → 0 errores; `npm run build` → OK (Vite).
- `bash scripts/verificar-despliegue.sh` → 5/5, cadena desplegada operativa.