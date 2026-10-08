# Pruebas local

Banco de pruebas local: validar contra **Supabase real** antes de desplegar. Requiere el `.env` del backend bien configurado (ver [entorno.md](entorno.md)).

## 0. Precondición: credenciales locales

- Backend `.env`: URL **pooler** + `DB_USER=postgres.akyvplcsiqhmfkqxvgoq` + password real.
- Frontend `.env`: `VITE_API_URL=http://localhost:8080`, `VITE_USER_ID=1`.

Comprobación sin levantar la app:

```bash
# SELECT 1 contra el pooler (debe responder "1")
PGSSLMODE=require PGPASSWORD=<password> \
PGOPTIONS="-c pooler_session_mode=transaction" \
psql -h aws-0-us-west-2.pooler.supabase.com -p 6543 \
  -U postgres.akyvplcsiqhmfkqxvgoq -d postgres -tAc "SELECT 1;"
```

## A. Prueba local e2e contra Supabase (ciclo crear → listar → borrar)

Levanta el backend y ejercita un ciclo completo de escritura real:

```bash
# Terminal 1: backend
./mvnw spring-boot:run
```

```bash
# Terminal 2: pruebas
curl -s http://localhost:8080/api/health
#   → {"status":"healthy","database":"connected",...}

# Crear evento de prueba (devuelve el DTO con idEvento)
curl -s -X POST http://localhost:8080/api/eventos -H "Content-Type: application/json" \
  -d '{"idUsuario":1,"idTipoEvento":1,"nombre":"Prueba E2E local","cliente":"Equipo","fechaEvento":"2026-12-01T10:00:00","lugar":"Sala de prueba"}'

# Aparece en el listado del usuario
curl -s "http://localhost:8080/api/eventos?usuarioId=1"

# Limpieza: borrar con el idEvento devuelto
curl -s -X DELETE http://localhost:8080/api/eventos/<idEvento> -w "HTTP %{http_code}\n"
#   → 204
```

> El `DELETE` es en cascada (borra las subtareas). Verificar que el evento ya no aparece en el listado.

## B. Frontend local contra backend local

```bash
# Terminal 1: backend
./mvnw spring-boot:run

# Terminal 2: frontend
cd ../planificador-eventos-frontend
npm install
npm run dev
# Abrir http://localhost:5173 y crear/editar/eliminar un evento desde la UI
```

CORS ya incluye `http://localhost:5173`, así que el flujo del navegador funciona sin cambios.

## C. Regresión automatizada

```bash
# Backend (JUnit + H2, perfil test, sin red)
./mvnw test

# Frontend (lint + build)
cd ../planificador-eventos-frontend
npm run lint
npm run build
```

## Checklist de salida

- [ ] `psql SELECT 1` responde.
- [ ] Backend local arranca con `ddl-auto=validate` sin errores.
- [ ] `/api/health` = `healthy` / `connected` local.
- [ ] Evento de prueba creado (201), listado y borrado (204).
- [ ] No quedó basura en Supabase (evento de prueba eliminado).
- [ ] `./mvnw test`, `npm run lint`, `npm run build` en verde.

## Notas

- Si la máquina **no tiene IPv6**, la vía directa (`db.<ref>.supabase.co`) NO conecta. Usar siempre el pooler.
- Escribir un evento en Supabase real es esperable aquí (se borra al terminar). Para evitar basura, borrar siempre el recurso creado.

## Bug conocido del pooler: `prepared statement "S_1" already exists`

Síntoma: `POST /api/eventos` (o cualquier INSERT/UPDATE) responde **500** con
`org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists`
solo localmente / de forma intermitente.

Causa: el pooler transaccional (PgBouncer) reutiliza sesiones de servidor; pgjdbc
crea prepared statements server-side con nombres `S_1`, `S_2`... que chocan entre
conexiones del pool.

Fix aplicado (2026-09-24): desactivar prepared statements server-side en
`application.properties`:

```properties
spring.datasource.hikari.data-source-properties.prepareThreshold=0
```

Verificación: tras el fix, el ciclo crear→listar→borrar pasó 201/200/204 y la
limpieza confirmó 0 eventos de prueba.
> Ojo: el despliegue en Render usa aún la imagen anterior (sin el fix). Funciona,
> pero es flaky: **recrear el servicio con el blueprint** para incluir el fix.