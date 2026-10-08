#!/usr/bin/env bash
# Smoke de la imagen Docker: valida que la imagen incluye el fix del pooler
# (prepareThreshold=0) y conecta a Supabase ANTES de desplegar a Render.
# Lee las credenciales del .env local (gitignored); no pide secretos.
#
# Uso: bash scripts/smoke-imagen.sh
set -uo pipefail
cd "$(dirname "$0")/.."

ROJO='\033[0;31m'; VERDE='\033[0;32m'; AMARILLO='\033[0;33m'; SIN='\033[0m'
PASS=0; FAIL=0
ok()  { PASS=$((PASS+1)); printf "${VERDE}  [OK]  %s${SIN}\n" "$1"; }
bad() { FAIL=$((FAIL+1)); printf "${ROJO}  [FAIL] %s${SIN}\n" "$1"; }
warn(){ printf "${AMARILLO}  [warn] %s${SIN}\n" "$1"; }

command -v docker >/dev/null || { bad "docker no está instalado"; exit 1; }

ENV_FILE="$PWD/.env"
[ -f "$ENV_FILE" ] || { bad "falta $ENV_FILE (copiar .env.example y completar)"; exit 1; }

DB_URL=$(grep '^DB_URL=' "$ENV_FILE" | head -1 | cut -d= -f2-)
DB_USER=$(grep '^DB_USER=' "$ENV_FILE" | head -1 | cut -d= -f2-)
DB_PASSWORD=$(grep '^DB_PASSWORD=' "$ENV_FILE" | head -1 | cut -d= -f2-)
[ -n "$DB_URL" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ] || {
  bad "DB_URL/DB_USER/DB_PASSWORD incompletos en $ENV_FILE"; exit 1; }

echo "$DB_URL" | grep -q "pooler" && ok "DB_URL usa pooler (recomendado)" \
  || warn "DB_URL es vía directa: solo conecta en hosts con IPv6"
echo "$DB_URL" | grep -q "6543" || warn "revisar el puerto del pooler (6543)"

IMAGE=pde-backend:verify
CNT=pde-verify
PORT="${SMOKE_PORT:-10000}"

echo "== 1. Build de la imagen =="
docker build -q -t "$IMAGE" . || { bad "build falló"; exit 1; }
ok "imagen $IMAGE construida"

echo "== 2. Arranque del contenedor (env del .env local) =="
docker rm -f "$CNT" >/dev/null 2>&1
docker run -d --name "$CNT" \
  -e PORT="$PORT" \
  -e DB_URL="$DB_URL" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JAVA_OPTS=-XX:MaxRAMPercentage=60 \
  -p "$PORT:$PORT" "$IMAGE" >/dev/null || { bad "no se pudo arrancar el contenedor"; exit 1; }

echo "== 3. /api/health =="
BODY=""
for i in 1 2 3 4 5; do
  sleep 4
  BODY=$(curl -s -m 5 "http://localhost:$PORT/api/health")
  echo "$BODY" | grep -q '"healthy"' && echo "$BODY" | grep -q '"connected"' && break
done
if echo "$BODY" | grep -q '"connected"'; then
  ok "/api/health → $BODY"
else
  bad "/api/health sin respuesta. Logs:"
  docker logs "$CNT" 2>&1 | tail -15
  docker rm -f "$CNT" >/dev/null 2>&1
  exit 1
fi

echo "== 4. Ciclo e2e en la imagen (crear → borrar) =="
RESP=$(curl -s -m 30 -X POST "http://localhost:$PORT/api/eventos" -H "Content-Type: application/json" \
  -d '{"idUsuario":1,"idTipoEvento":1,"nombre":"Smoke imagen temporal","cliente":"QA","fechaEvento":"2026-12-03T10:00:00","lugar":"Contenedor"}')
ID=$(echo "$RESP" | grep -o '"idEvento":[0-9]*' | head -1 | cut -d: -f2)
if [ -n "$ID" ]; then
  ok "POST creó evento id=$ID"
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 30 -X DELETE "http://localhost:$PORT/api/eventos/$ID")
  [ "$CODE" = "204" ] && ok "DELETE → 204 (escribir/borrar OK, fix pooler activo)" \
    || bad "DELETE → $CODE"
else
  bad "POST falló. Logs:"
  docker logs "$CNT" 2>&1 | tail -15
fi

echo "== 5. Buscar el error conocido del pooler =="
if docker logs "$CNT" 2>&1 | grep -q "prepared statement" ; then
  bad "aparece 'prepared statement' en logs: revisar prepareThreshold=0"
else
  ok "sin errores de prepared statements en logs"
fi

echo "== Limpieza =="
docker rm -f "$CNT" >/dev/null 2>&1
ok "contenedor $CNT eliminado"

echo
echo "Resumen: $PASS correctos, $FAIL fallidos."
if [ "$FAIL" -eq 0 ]; then echo "SMOKE OK: imagen lista para desplegar."; else echo "SMOKE FALLÓ: no desplegar."; fi
exit "$FAIL"