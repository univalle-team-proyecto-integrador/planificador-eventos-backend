---
tipo: mejora
---
# Workflow de desarrollo descentralizado + smoke de imagen

- **Fecha:** 2026-09-24
- **Área:** infraestructura / proceso
- **Estado:** hecha

## Descripción

Se formalizó la manera de desarrollar de forma descentralizada ("producción primero"): cualquier integrante puede llevar un cambio completo (desarrollar → probar → desplegar → verificar → registrar) sin depender de una sola persona. Se añadió el playbook auto-servicio y un script de smoke de imagen que valida el fix del pooler ANTES de desplegar. Se documentó además el procedimiento de rotación de la password (pendiente de ejecutar por seguridad).

## Cambios

- `test/workflow-desarrollo.md` (nuevo) — playbook paso a paso: roles, entorno, pruebas, e2e local, smoke de imagen, deploy en Render (dos casos: redeploy normal vs Blueprint si cambia `render.yaml`), verificación y registro en bóveda. Incluye la **rotación de password** con bitácora.
- `scripts/smoke-imagen.sh` (nuevo) — construye la imagen Docker, la corre contra Supabase (pooler, credenciales leídas del `.env` local), hace `/api/health` + ciclo crear/borrar y verifica que no aparezca `prepared statement` en logs. No pide secretos.
- `test/README.md` — mapa actualizado con `workflow-desarrollo.md` y la sección de scripts.
- `AGENTS.md` — referencia al workflow descentralizado y al `smoke-imagen.sh`.
- `boveda/mejoras/README.md` — fila 016.
- `boveda/lienzo-maestro.canvas` — nodo enlazado.

## Verificación

- `bash scripts/smoke-imagen.sh` → **7/7**: pooler OK, imagen construida, `/api/health` healthy/connected, POST creó evento `id=6`, DELETE 204 (fix `prepareThreshold=0` activo), sin errores de prepared statements, limpieza OK. → `SMOKE OK: imagen lista para desplegar`.
- `bash scripts/verificar-despliegue.sh` → 5/5 (cadena desplegada operativa).
- `./mvnw test` → 35 tests, 0 fallos (verificado previamente en mejora 015).
- `git diff --check` → sin errores.

## Pendientes

- **Rotación de la password** (filtrada en chat 2026-09-24): procedimiento listo en `test/workflow-desarrollo.md`, bitácora `(pendiente)`.
- **Redeploy de Render** con la imagen que incluye `prepareThreshold=0`: `Manual Deploy → Deploy latest commit` sobre `planificador-eventos-backend-1` (la imagen actual ya fue validada por el smoke).