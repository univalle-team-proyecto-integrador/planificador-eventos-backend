# Pruebas y validación del despliegue

Documentación descentralizada de cómo validar la conexión **frontend (Vercel) → backend (Render) → Supabase** y cómo probar el stack localmente antes de desplegar.

> Meta del equipo: **producción primero**. Lo local es banco de pruebas; cuando funciona, se despliega.

## Mapa del folder

| Documento | Qué resuelve |
| --- | --- |
| [workflow-desarrollo.md](workflow-desarrollo.md) | **Playbook auto-servicio**: cómo cualquier integrante desarrolla → prueba → despliega → verifica → registra. Incluye rotación de password. |
| [entorno.md](entorno.md) | Variables de entorno: qué va en cada `.env` (backend local, Render, Vercel) y las dos vías de conexión a Supabase. |
| [conexion.md](conexion.md) | Arquitectura de conexión: URLs reales, flujo de datos, CORS y el fallback del frontend. |
| [validacion-despliegue.md](validacion-despliegue.md) | Ruta de verificación de la cadena **desplegada** (script + paso a paso). |
| [documentacion-swagger.md](documentacion-swagger.md) | Documentación oficial de la API con **Swagger** (UI desplegada, spec OpenAPI y evidencia). |
| [postman.md](postman.md) | Pruebas del **backend** con **Postman** (colección lista en `test/postman/`). |
| [frontend-chrome.md](frontend-chrome.md) | Pruebas del **frontend** en **Chrome** (checklist de flujos + Network). |
| [pruebas-local.md](pruebas-local.md) | Plan de pruebas **local**: banco de pruebas contra Supabase real y regresión. |
| [validacion-e2e-sprint1.md](validacion-e2e-sprint1.md) | Acta de validación **end-to-end del Sprint 1**: mapa historia → evidencia y resultados. |
| [terminos-clave.md](terminos-clave.md) | Glosario: ref, vía directa vs pooler, publishable key, cold start, etc. |
| [plantilla-caso-prueba.md](plantilla-caso-prueba.md) | Plantilla para registrar un caso de prueba nuevo. |

Carpeta `test/postman/`: colección `planificador-eventos-backend.postman_collection.json` y entorno `planificador-eventos-backend.postman_environment.json`, listos para **Import** en Postman.

Carpeta `test/evidencia-jira-sprint1/`: evidencia del tablero Jira del Sprint 1 (captura/export; ver su `README.md`).

Scripts asociados (en `scripts/`):

| Script | Uso |
| --- | --- |
| `scripts/verificar-despliegue.sh` | Valida la cadena **desplegada** (health → datos → CORS → bundle). |
| `scripts/smoke-imagen.sh` | Valida la **imagen Docker local** (incluye el fix del pooler) antes de desplegar. |

## Cómo usar este folder

1. **Validar la cadena desplegada** (después de cada deploy) → `bash scripts/verificar-despliegue.sh`. Detalles en [validacion-despliegue.md](validacion-despliegue.md).
2. **Probar local** (antes de desplegar) → sigue [pruebas-local.md](pruebas-local.md).
3. **Configurar credenciales** (nueva máquina o entorno) → [entorno.md](entorno.md).
4. **Dudar de un término** → [terminos-clave.md](terminos-clave.md).

Reglas: no copiar secretos en estos archivos (los `.env` están gitignored); los comandos son los que se validaron en la mejora [2026-09-24-014-validacion-conexion-front-back-db](../boveda/mejoras/2026-09-24-014-validacion-conexion-front-back-db.md).