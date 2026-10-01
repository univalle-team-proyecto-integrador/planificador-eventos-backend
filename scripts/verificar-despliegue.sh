#!/usr/bin/env bash
# Verifica la cadena completa frontend (Vercel) → backend (Render) → Supabase.
# Uso: bash scripts/verificar-despliegue.sh
set -uo pipefail

BACKEND="${PDE_BACKEND:-https://planificador-eventos-backend-1.onrender.com}"
FRONTEND="${PDE_FRONTEND:-https://planificador-eventos-frontend-ten.vercel.app}"

# Cuenta fija de verificación para US-11. Se reutiliza en cada ejecución: el
# primer intento la crea y los siguientes solo inician sesión. Se puede evitar
# por completo pasando un token ya válido en PDE_VERIFY_TOKEN.
VERIFY_USER="${PDE_VERIFY_USER:-verificacion.despliegue@eventflow.co}"
VERIFY_PASS="${PDE_VERIFY_PASS:-Despliegue2026}"

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

echo "== 2. Autenticación US-11: registro/login, perfil y token =="
TOKEN="${PDE_VERIFY_TOKEN:-}"
AUTH_OK=0
if [ -z "$TOKEN" ]; then
  # Registrar primero: si la cuenta ya existe responde 409 y no es un fallo.
  registro=$(curl -s -m 60 -X POST "$BACKEND/api/users/register" \
    -H "Content-Type: application/json" \
    -d "{\"nombre\":\"Verificacion de despliegue\",\"email\":\"$VERIFY_USER\",\"password\":\"$VERIFY_PASS\"}")
  if echo "$registro" | grep -q '"token"'; then
    TOKEN=$(echo "$registro" | jq -r '.token')
    ok "registro devolvió token"
  elif echo "$registro" | grep -q 'correo'; then
    warn "la cuenta de verificación ya existe; se inicia sesión"
  else
    bad "registro inesperado → ${registro:-sin respuesta}"
  fi

  if [ -z "$TOKEN" ]; then
    login=$(curl -s -m 60 -X POST "$BACKEND/api/users/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$VERIFY_USER\",\"password\":\"$VERIFY_PASS\"}")
    TOKEN=$(echo "$login" | jq -r '.token // empty')
    [ -n "$TOKEN" ] && ok "login devolvió token" || bad "login falló → ${login:-sin respuesta}"
  fi
else
  ok "usando PDE_VERIFY_TOKEN del entorno"
fi

perfil=$(curl -s -m 60 "$BACKEND/api/users/profile" -H "Authorization: Bearer $TOKEN")
if echo "$perfil" | grep -q '"email"'; then
  AUTH_OK=1
  ok "/api/users/profile responde con el usuario del token"
elif echo "$perfil" | grep -q '"status":401'; then
  bad "/api/users/profile → 401: el token no sirve en el backend desplegado (¿JWT_SECRET distinto?)"
elif echo "$perfil" | grep -q '"status":404'; then
  bad "/api/users/profile → 404: el backend desplegado no tiene US-11 (falta hacer push y redesplegar)"
else
  bad "/api/users/profile inesperado → ${perfil:-sin respuesta}"
fi

echo "== 3. Supabase real: /api/tipos-evento y /api/eventos con token =="
tipos=$(curl -s -m 90 "$BACKEND/api/tipos-evento")
[ -n "$tipos" ] && [ "$tipos" != "[]" ] && ok "catálogo devuelve datos (${#tipos} bytes)" || bad "catálogo vacío o sin respuesta"

# Sin un token válido los datos no dicen nada: un backend viejo con
# PROTECT_SUBTAREAS=false responde igual sin autenticación y parecería sano.
if [ "$AUTH_OK" -ne 1 ]; then
  bad "omitido: no se pudo autenticar, así que /api/eventos y /api/subtareas/hoy no son verificables (revisa el paso 2)"
else

# Un ProblemDetail 401 también es "no vacío", así que comparar con [] no basta:
# hay que comprobar que la respuesta es un arreglo JSON de verdad.
eventos=$(curl -s -m 90 "$BACKEND/api/eventos" -H "Authorization: Bearer $TOKEN")
case "$eventos" in
  \[*) if [ "$eventos" != "[]" ]; then
        ok "/api/eventos devuelve datos del titular del token (${#eventos} bytes)"
      else
        warn "/api/eventos vacío: normal si la cuenta de verificación no tiene eventos"
      fi ;;
  *)  bad "/api/eventos no devolvió un arreglo (revisa si exige token) → $(echo "$eventos" | head -c 200)" ;;
esac

# usuarioId ya no es obligatorio: el propietario sale del token. Se omite a
# propósito, para comprobar que el contrato nuevo está desplegado y que un
# despliegue viejo (que exigía el parámetro) se nota.
hoy=$(curl -s -m 90 "$BACKEND/api/subtareas/hoy?fecha=$(date +%F)" \
  -H "Authorization: Bearer $TOKEN")
# Solo un arreglo es válido: un ProblemDetail empieza por { y un 401 pasaría
# por una respuesta sana si no se distingue.
case "$hoy" in
  \[*) ok "/api/subtareas/hoy responde con el token (${#hoy} bytes)" ;;
  *)    bad "/api/subtareas/hoy → $(echo "$hoy" | head -c 200)" ;;
esac

# El usuarioId del query se ignora por diseño: el token manda. Esta comprobación
# solo avisa, porque exigir 200 aquí sería depender de un token ajeno.
ajeno=$(curl -s -m 90 "$BACKEND/api/eventos?usuarioId=1" -H "Authorization: Bearer $TOKEN")
case "$eventos" in
  \[*) if [ "$ajeno" = "$eventos" ]; then
         ok "el usuarioId del query se ignora: misma respuesta con usuarioId=1"
       else
         warn "el usuarioId del query cambió la respuesta; revisa el aislamiento por propietario"
       fi ;;
  *) bad "no se puede comprobar el aislamiento: /api/eventos no devolvió un arreglo" ;;
esac

# Estado de PROTECT_SUBTAREAS. Mientras sea false, /api/subtareas/** acepta
# peticiones anónimas y las resuelve con LEGACY_USER_ID: es la ventana de
# compatibilidad y no es un fallo. Con PDE_ESPERAR_PROTECCION=true se exige
# que la ventana ya esté cerrada, que es el estado final de US-11.
sin_token=$(curl -s -m 60 -o /dev/null -w "%{http_code}" \
  "$BACKEND/api/subtareas/hoy?fecha=$(date +%F)")
if [ "$sin_token" = "401" ]; then
  ok "/api/subtareas/hoy rechaza peticiones sin token (PROTECT_SUBTAREAS activo)"
elif [ "${PDE_ESPERAR_PROTECCION:-false}" = "true" ]; then
  bad "/api/subtareas/hoy respondió $sin_token sin token: PROTECT_SUBTAREAS sigue en false"
  echo "      -> Render -> Environment -> PROTECT_SUBTAREAS=true -> Save"
else
  warn "/api/subtareas/hoy respondió $sin_token sin token: PROTECT_SUBTAREAS=false (ventana de compatibilidad abierta)"
fi
fi

echo "== 4. CORS desde el frontend real =="
hdrs=$(curl -s -i -X OPTIONS -m 60 "$BACKEND/api/eventos" \
  -H "Origin: $FRONTEND" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: Content-Type")
echo "$hdrs" | grep -qi "access-control-allow-origin: $FRONTEND" && ok "preflight autoriza $FRONTEND" || bad "preflight sin access-control-allow-origin"

echo "== 5. Frontend apunta al backend correcto (bundles) =="
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