# Workflow de desarrollo descentralizado

Guía **auto-servicio** para que cualquier integrante del equipo lleve un cambio completo de punta a punta: desarrollar → probar → desplegar → verificar → registrar. No se depende de una sola persona para pasos repetibles.

## Roles

| Rol | Qué hace |
| --- | --- |
| **Desarrollador/a** | Trabaja en su rama (`backend/lead`, `frontend/lead`), corre TODO este flujo en su cuenta y deja el despliegue verificado. |
| **Coordinador/QA** | Hace el merge a `main` y el QA final (re-corre `verificar-despliegue.sh`). |

## Políticas

- **Producción primero**: lo local es banco de pruebas; cuando funciona, se despliega.
- Cada cambio que toque código/config/infra se documenta en la bóveda (`boveda/mejoras/`).
- Los secretos viven solo en: `.env` local (gitignored), Render dashboard, Supabase. Nunca en git ni en chats.

## Flujo por cambio (descentralizado)

### 1. Preparar el entorno
- Configurar `.env` local según [entorno.md](entorno.md) y comprobar credenciales:
  ```bash
  PGSSLMODE=require PGPASSWORD=<password> \
  PGOPTIONS="-c pooler_session_mode=transaction" \
  psql -h aws-0-us-west-2.pooler.supabase.com -p 6543 \
    -U postgres.akyvplcsiqhmfkqxvgoq -d postgres -tAc "SELECT 1;"
  ```
  → responde `1`.

### 2. Desarrollar
- En `backend/lead` (back) o `frontend/lead` (front). Commits en español.

### 3. Probar local (regresión)
- Backend: `./mvnw test` → 35+, 0 fallos. Frontend: `npm run lint` y `npm run build`.
- Guardar la evidencia en la mejora de la bóveda.

### 4. Prueba e2e local contra Supabase
- Seguir [pruebas-local.md](pruebas-local.md): ciclo crear → listar → borrar (201/200/204) y limpieza confirmada.
- **Checklist fix pooler**: si el POST devuelve `prepared statement "S_1" already exists`, aplicar/verificar `prepareThreshold=0` (ver `pruebas-local.md`).

### 5. Smoke de la imagen Docker (antes de desplegar)
- Validar que la imagen que subirá incluye el fix y conecta bien:
  ```bash
  bash scripts/smoke-imagen.sh
  ```
- Debe terminar con `SMOKE OK: imagen lista para desplegar`.

### 6. Desplegar backend (Render)
El servicio actual es `planificador-eventos-backend-1`. Dos casos:

- **Cambio normal de código/config** (como `application.properties`): la imagen se reconstruye con el deploy.
  1. Merge a `main` (coordinador).
  2. Render → servicio `planificador-eventos-backend-1` → **Manual Deploy → Deploy latest commit**.
- **Cambio en `render.yaml`** (URLs, env vars, blueprint): el blueprint NO se auto-sincroniza.
  1. Render → **New → Blueprint** para crear/recrear el servicio.
  2. Fijar a mano en el dashboard: `DB_PASSWORD` (`sync:false`), y recordar `JAVA_OPTS`.

Cualquier integrante con acceso al proyecto Render puede ejecutarlo.

### 7. Verificar la cadena desplegada
```bash
bash scripts/verificar-despliegue.sh
```
→ `Resumen: 5 correctos, 0 fallidos.` (health → datos Supabase → CORS → bundle).

### 8. Registrar en la bóveda
- Crear archivo en `boveda/mejoras/AAAA-MM-DD-NNN-breve-slug.md` (plantilla → `plantilla-mejora.md`).
- Actualizar índice `boveda/mejoras/README.md` y el `boveda/lienzo-maestro.canvas`.

## Rotación de la password de la base (parte 2)

Procedimiento manual (no se automatiza por seguridad). Ejecutar cuando una password se haya filtrado (p. ej. compartida en chat) o por política.

1. **Supabase** → Project → Database → *Reset database password* (o new password en la cadena de conexión). Anotar la nueva en el gestor de secretos del equipo.
2. **Render** → servicio `planificador-eventos-backend-1` → Environment → `DB_PASSWORD` → fijar la nueva a mano (`sync:false`). Guardar (replica el servicio con la nueva).
3. **Local** → `planificador-eventos-backend/.env` → actualizar `DB_PASSWORD` (nunca commitear).
4. **Validar**:
   ```bash
   bash scripts/verificar-despliegue.sh   # cadena desplegada
   bash scripts/smoke-imagen.sh           # imagen local conecta (opcional)
   ```
5. **Registrar** en la bóveda: fecha de rotación, quién, alcance. Dejar nota aquí abajo.

### Bitácora de rotaciones

| Fecha | Motivo | Quién | Estado |
| --- | --- | --- | --- |
| *(pendiente)* | password compartida en chat (2026-09-24) | — | por hacer |

> Si la rotación cambia la password y algún script/documento la referencia, actualizar también `render.yaml`... no: **nunca** poner la password en `render.yaml` (se fija en dashboard).