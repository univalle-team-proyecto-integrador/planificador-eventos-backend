# Entorno: qué va en cada `.env`

La configuración es **12-factor**: cada entorno aporta las variables completas. Los `.env` están **gitignored** — nunca se commitean. `.env.example` solo tiene placeholders.

## Referencia rápida

| Archivo | Variables | Comentario |
| --- | --- | --- |
| Backend local (`planificador-eventos-backend/.env`) | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión JDBC a Supabase. Usar **pooler** (este PC no tiene IPv6). |
| Render (`render.yaml` + dashboard) | `DB_URL`, `DB_USER`, `JAVA_OPTS`; `DB_PASSWORD` **a mano en el dashboard** | Ya configurado y funcionando. No tocar. |
| Frontend local (`planificador-eventos-frontend/.env`) | `VITE_API_URL`, `VITE_USER_ID` | Solo apuntan al **backend**, nunca a Supabase. |
| Vercel (dashboard → Environment Variables) | `VITE_API_URL`, `VITE_USER_ID` | **Deben conservar el prefijo `VITE_`** para que Vite las incruste en el bundle. |

## Backend `.env` (local)

```dotenv
# Pooler transaccional — recomendado para cualquier red IPv4/IPv6.
DB_URL=jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&options=-c%20pooler_session_mode%3Dtransaction
DB_USER=postgres.akyvplcsiqhmfkqxvgoq
DB_PASSWORD=<password real de la base>
```

```dotenv
# Vía directa — SOLO en máquinas con IPv6 (DNS resuelve solo IPv6).
DB_URL=jdbc:postgresql://db.akyvplcsiqhmfkqxvgoq.supabase.co:5432/postgres?sslmode=require
DB_USER=postgres
DB_PASSWORD=<misma password real>
```

> La **password real** se obtiene de Supabase (Database → Connection string) o del dashboard de Render (`DB_PASSWORD`, ya puesta ahí). La publishable key (`sb_publishable_...`) **no** es la password ni se usa en este proyecto.
>
> **Importante:** el pooler exige `prepareThreshold=0` para no chocar con las prepared statements de pgjdbc (bug `prepared statement "S_1" already exists`). Ya está fijado por `spring.datasource.hikari.data-source-properties.prepareThreshold=0`, no hace falta tocar la URL.

## Render (despliegue)

- `render.yaml` ya define `DB_URL`, `DB_USER` y `JAVA_OPTS` (pooler).
- `DB_PASSWORD` se fija a mano: Render → servicio → Environment. Es la única vía (no se commitea).
- Tras cambios en `render.yaml`, recrear el servicio con **New → Blueprint** (no hay auto-sync).

## Frontend `.env` (local)

```dotenv
# Backend local levantado con ./mvnw spring-boot:run
VITE_API_URL=http://localhost:8080

# Usuario temporal del organizador (hasta implementar autenticación)
VITE_USER_ID=1
```

## Vercel (despliegue del frontend)

- `VITE_API_URL=https://planificador-eventos-backend-1.onrender.com` y `VITE_USER_ID=1`.
- **Sin el prefijo `VITE_` la variable no llega al navegador** (Vite solo expone `VITE_*`). No pasa nada catastrófico porque existe el fallback en `api.js`, pero la variable queda inerte.
- Vite incrusta las variables al **subir el build**: después de cambiarlas hay que *Redeploy*.
- No poner aquí password de BD ni ningún secreto (van al bundle, son públicas).

## Diagrama de responsabilidad de la ref

La ref del proyecto es `akyvplcsiqhmfkqxvgoq` (de `https://akyvplcsiqhmfkqxvgoq.supabase.co`).

- En el **pooler**: va en el usuario → `postgres.akyvplcsiqhmfkqxvgoq`.
- En la **vía directa**: va en el host → `db.akyvplcsiqhmfkqxvgoq.supabase.co`.
- En el **frontend**: no va en ningún lado. El frontend nunca se conecta a la BD.