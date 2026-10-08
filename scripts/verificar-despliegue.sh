#!/usr/bin/env bash
# Verifica la cadena completa frontend (Vercel) → backend (Render) → Supabase.
# Uso: bash scripts/verificar-despliegue.sh
set -uo pipefail

BACKEND="https://planificador-eventos-backend-1.onrender.com"
FRONTEND="https://planificador-eventos-frontend-ten.vercel.app"

ROJO='\033[0;31m'; VERDE='\033[0;32m'; AMARILLO='\033[0;33m'; SIN='\033[0m'
PASS=0; FAIL=0

ok()  { PASS=$((PASS+1)); printf "${VERDE}  [OK]  %s${SIN}\n" "$1"; }
bad() { FAIL=$((FAIL+1)); printf "${ROJO}  [FAIL] %s${SIN}\n" "$1"; }
warn(){ printf "${AMARILLO}  [warn] %s${SIN}\n" "$1"; }

echo "== 1. Backend vivo: /api/health =="
body=""
for i in 1 2 3; do
  body=$(curl -s -m 90 "$BACKEND/api/health")
  if [ -n "$body" ] && [ "$(echo "$body" | grep -c '"healthy"')" -gt 0 ] && [ "$(echo "$body" | grep -c '"connected"')" -gt 0 ]; then
    break
  fi
  [ "$i" -lt 3 ] && warn "intento $i (posible cold start ~35 s), reintentando..."
done
if [ "$(echo "$body" | grep -c 'healthy')" -gt 0 ] && [ "$(echo "$body" | grep -c 'connected')" -gt 0 ]; then ok "/api/health → $body"; else bad "/api/health → ${body:-sin respuesta}"; fi

echo "== 2. Supabase real: /api/tipos-evento y eventos del usuario 1 =="
tipos=$(curl -s -m 90 "$BACKEND/api/tipos-evento")
eventos=$(curl -s -m 90 "$BACKEND/api/eventos?usuarioId=1")
[ -n "$tipos" ] && [ "$tipos" != "[]" ] && ok "catálogo devuelve datos (${#tipos} bytes)" || bad "catálogo vacío o sin respuesta"
[ -n "$eventos" ] && [ "$eventos" != "[]" ] && ok "eventos del usuario 1 devuelven datos (${#eventos} bytes)" || warn "eventos del usuario 1 vacíos (puede ser normal)"

echo "== 3. CORS desde el frontend real =="
hdrs=$(curl -s -i -X OPTIONS -m 60 "$BACKEND/api/eventos" \
  -H "Origin: $FRONTEND" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: Content-Type")
echo "$hdrs" | grep -qi "access-control-allow-origin: $FRONTEND" && ok "preflight autoriza $FRONTEND" || bad "preflight sin access-control-allow-origin"

echo "== 4. Frontend apunta al backend correcto (bundle) =="
index=$(curl -sL -m 30 "$FRONTEND")
js=$(echo "$index" | grep -o 'src="[^"]*\.js"' | head -1 | sed 's/src="//;s/"//')
if [ -n "$js" ]; then
  curl -sL -m 60 "$FRONTEND$js" | grep -q "$BACKEND" \
    && ok "bundle $js contiene $BACKEND" \
    || bad "bundle $js NO contiene $BACKEND"
else
  bad "no se encontró bundle JS en $FRONTEND"
fi

echo
echo "Resumen: $PASS correctos, $FAIL fallidos."
[ "$FAIL" -eq 0 ] && echo "Cadena completa operativa." || echo "Hay fallos: revisar la salida anterior."
exit "$FAIL"