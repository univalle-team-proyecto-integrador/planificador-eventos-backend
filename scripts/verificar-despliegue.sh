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

echo "== 4. Frontend apunta al backend correcto (bundles) =="
# Vite separa el cliente HTTP en un chunk lazy (assets/api-*.js) que NO aparece
# como script en el HTML inicial: hay que recorrer el grafo de imports.
normalizar() {
  local pila=() parte
  for parte in $(echo "$1" | tr '/' '\n'); do
    case "$parte" in
      ""|".") ;;
      "..") if [ "${#pila[@]}" -gt 0 ]; then pila=("${pila[@]:0:${#pila[@]}-1}"); fi ;;
      *) pila+=("$parte") ;;
    esac
  done
  local salida=""
  for parte in "${pila[@]:-}"; do salida="$salida/$parte"; done
  echo "$salida"
}

resolver() {
  case "$1" in
    /*) echo "$1" ;;
    http*) echo "$1" ;;
    ./*|../*) normalizar "$(dirname "$2")/$1" ;;
    assets/*) echo "/$1" ;;
    *) echo "" ;;
  esac
}

index=$(curl -sL -m 30 "$FRONTEND")
entrada=$(echo "$index" | grep -oE 'src="[^"]+\.js"' | head -1 | sed 's/src="//;s/"//')
vistos=""
encontrado=""
cola="$entrada"
while [ -n "$(echo "$cola" | tr -s ' ')" ]; do
  actual=$(echo "$cola" | tr -s ' ' '\n' | grep -v '^$' | head -1)
  cola=$(echo "$cola" | tr -s ' ' '\n' | grep -v '^$' | tail -n +2 | tr '\n' ' ')
  [ -z "$actual" ] && continue
  case " $vistos " in *" $actual "*) continue ;; esac
  vistos="$vistos $actual"
  [ "$(echo "$vistos" | wc -w)" -gt 40 ] && { warn "límite de 40 bundles alcanzado"; break; }
  cuerpo=$(curl -sL -m 60 "$FRONTEND$actual")
  echo "$cuerpo" | grep -q "$BACKEND" && encontrado="$encontrado $actual"
  for token in $(echo "$cuerpo" | grep -aoE '[A-Za-z0-9_./-]*\.js' | sort -u); do
    hijo=$(resolver "$token" "$actual")
    [ -n "$hijo" ] && cola="$cola $hijo"
  done
done
total=$(echo "$vistos" | wc -w)
if [ -z "$entrada" ]; then
  bad "no se encontró bundle JS en $FRONTEND"
elif [ -n "$encontrado" ]; then
  ok "$BACKEND referenciado en:$encontrado (de $total bundles revisados)"
else
  bad "ningún bundle de $total revisados referencia $BACKEND"
fi

echo
echo "Resumen: $PASS correctos, $FAIL fallidos."
[ "$FAIL" -eq 0 ] && echo "Cadena completa operativa." || echo "Hay fallos: revisar la salida anterior."
exit "$FAIL"