# Validación de la cadena desplegada

Objetivo: confirmar que **frontend desplegado → backend desplegado → Supabase** están conectados.

## Comando único

```bash
bash scripts/verificar-despliegue.sh
```

## Qué comprueba (en orden)

| # | Paso | Verificación |
| --- | --- | --- |
| 1 | Backend vivo | `GET /api/health` → `healthy` + `connected`. Reintenta 3 veces (cold start). |
| 2 | Supabase real | `GET /api/tipos-evento` y `GET /api/eventos?usuarioId=1` devuelven datos (no `[]`). |
| 3 | CORS | Preflight `OPTIONS` con `Origin: https://planificador-eventos-frontend-ten.vercel.app` → `access-control-allow-origin` correcto. |
| 4 | Bundle del frontend | El JS servido por Vercel contiene `planificador-eventos-backend-1.onrender.com`. |

Salida esperada al final: `Resumen: 5 correctos, 0 fallidos.` y `Cadena completa operativa.`

## Paso a paso manual (equivalente)

```bash
# 1. Backend y BD
curl -s https://planificador-eventos-backend-1.onrender.com/api/health
#   → {"status":"healthy","database":"connected",...}

# 2. Datos reales
curl -s https://planificador-eventos-backend-1.onrender.com/api/tipos-evento
curl -s "https://planificador-eventos-backend-1.onrender.com/api/eventos?usuarioId=1"

# 3. CORS (preflight)
curl -s -i -X OPTIONS https://planificador-eventos-backend-1.onrender.com/api/eventos \
  -H "Origin: https://planificador-eventos-frontend-ten.vercel.app" \
  -H "Access-Control-Request-Method: GET" | grep -i access-control

# 4. Bundle apunta al backend correcto
curl -sL https://planificador-eventos-frontend-ten.vercel.app | grep -o 'src="[^"]*\.js"' | head -1
curl -sL <bundle_url> | grep -o "planificador-eventos-backend-1\.onrender\.com"
```

## Interpretación de resultados

- **504 / timeout en el paso 1**: el plan free de Render duerme; esperar ~35 s y reintentar (el script ya lo hace). Si además el primer request da un 500 transitorio, reintentar: es normal tras despertar frío.
- **`database` distinto de `connected`**: revisar credenciales para Render (ver [entorno.md](entorno.md)).
- **`access-control-allow-origin` ausente en el paso 3**: revisar `app.cors.allowed-origins` y el origen exacto del frontend.
- **Paso 4 sin la URL esperada**: el bundle no se buildéó con el backend correcto; revisar `VITE_API_URL` en Vercel (prefijo `VITE_` obligatorio) y *Redeploy*.

## Cuándo correr

- Tras cada **redeploy** del backend o del frontend.
- Tras cambios de **`application.properties`** (CORS) o del **`render.yaml`** (recrear servicio con Blueprint).
- Como smoke test antes de dar una funcionalidad por cerrada.