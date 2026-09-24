# Glosario clave

Términos usados en el proyecto para no confundirse al configurar entornos.

| Término | Significado |
| --- | --- |
| **Ref del proyecto** | Identificador de tu proyecto Supabase: `akyvplcsiqhmfkqxvgoq`. Aparece en `https://akyvplcsiqhmfkqxvgoq.supabase.co` (dashboard). |
| **Vía directa** | Conexión a `db.<ref>.supabase.co:5432`, usuario `postgres`. Su DNS **solo resuelve IPv6** → funciona solo donde hay IPv6 (falla en este PC, Docker y Render). |
| **Pooler transaccional** | Conexión a `aws-0-us-west-2.pooler.supabase.com:6543`, usuario `postgres.<ref>`. IPv4/dual-stack → alcanzable desde cualquier red. Requiere `options=-c pooler_session_mode=transaction` en el `DB_URL`. |
| **DB_PASSWORD** | Password real de la base (la de la cadena de conexión de Supabase). Igual para la vía directa y el pooler. Se usa en backend/.env local y en el dashboard de Render. |
| **Publishable key / anon key** | Clave `sb_publishable_...` del SDK de Supabase. Es **pública** y este proyecto **no la usa** (nuestro frontend llama a la API Spring, no al SDK). No es la password. |
| **VITE_ prefix** | Prefijo obligatorio para que Vite exponga una variable al bundle del navegador (`import.meta.env.VITE_*`). Sin él, la variable queda inerte. |
| **Fallback de api.js** | URL fija `https://planificador-eventos-backend-1.onrender.com` que usa el frontend si `VITE_API_URL` no está. |
| **Cold start (Render free)** | Al dormirse, el primer request tarda ~35 s en despertar y puede devolver un 500 transitorio. Reintentar. |
| **`-1` vs `-0` en Render** | Al recrear el servicio con Blueprint, Render crea subdominio con sufijo. El Spring vive en `...-1`; el `...-0` es otra API (no Spring). |
| **`-ten` vs `-` en Vercel** | El proyecto real del frontend es `planificador-eventos-frontend-ten`; el `planificador-eventos-frontend` (sin sufijo) aloja otro build (Create React App). |
| **ddl-auto=validate** | Hibernate valida el esquema contra la BD remota al arrancar, nunca lo modifica. El esquema se define a mano en `db/ddl-supabase.sql`. |
| **12-factor** | Config por variables de entorno (url+user+password completos), sin código de configuración por ambiente. |
| **Swagger/OpenAPI** | Documentación interactiva de la API servida por el propio backend (`/swagger-ui.html`, spec en `/v3/api-docs`). No es un puente: el front llama directo a `/api/**`; el *Try it out* va al mismo host (servidor relativo `/`), por eso no hay CORS. |