---
tipo: mejora
---

# US-11: autenticación JWT y aislamiento real por propietario

- **Fecha:** 2026-09-29
- **Área:** seguridad, API, frontend
- **Estado:** hecha

## Descripción

Se implementó US-11 de punta a punta: autenticación **stateless con JWT** en el backend, aislamiento de datos por propietario (no por parámetro) y la parte visual de login/registro en el frontend, que hasta ahora pedía solo el correo y "entraba" sin contraseña.

Hasta esta mejora, cualquier cliente podía listar todos los eventos (`GET /api/eventos` sin filtro devolvía `findAll()`), leer, editar y borrar eventos y subtareas de cualquier cuenta, y crear eventos a nombre de otro usuario enviando otro `idUsuario` en el cuerpo. Todo eso era un IDOR, no una decisión de diseño.

### Decisiones de seguridad

1. **Stateless, sin cookies.** HS256 con Nimbus; el token viaja en `Authorization: Bearer`. No hay sesión en el servidor ni cookies `JSESSIONID`.
2. **El propietario sale del token, nunca del parámetro.** `CurrentUserProvider` resuelve la identidad efectiva; los `usuarioId` del query y del cuerpo se siguen aceptando para no romper clientes, pero **se ignoran**.
3. **404 y no 403 para lo ajeno.** Un 403 confirmaría que ese id existe en la cuenta de otra persona, que es medio camino para enumerar datos ajenos. El mensaje es el mismo que el de un id inexistente.
4. **Candado arquitectónico en los repositorios.** `EventoRepository` y `SubtareaRepository` extienden `Repository`, no `JpaRepository`/`CrudRepository`. Así `findAll()` y `findById(id)` literalmente **no existen** y no se puede reintroducir una fuga sin que el código ni compile. Dos pruebas por reflexión lo vigilan.
5. **Despliegue gradual.** `PROTECT_SUBTAREAS=false` mantiene abiertas las rutas de subtareas y acota los datos al usuario legado, para poder subir el backend sin romper el frontend aún sin token.

### Side effect que conviene conocer

Cuando `PROTECT_SUBTAREAS=true`, `CurrentUserProvider` **deja de devolver el usuario legado** y lanza 401. Como todas las operaciones de eventos resuelven el propietario por ahí, `/api/eventos/**` queda de hecho protegida aunque su matcher en la cadena de seguridad siga en `permitAll()`. No es un descuido: la protección real es la resolución de propietario, el matcher solo cubre la familia `/api/subtareas/**`.

## Cambios

### Backend — dependencias y configuración

- `pom.xml` — `spring-boot-starter-security`, `spring-security-oauth2-jose` y `spring-boot-starter-security-test` (ojo: en Spring Boot 4 el sufijo es `-test`, no `spring-security-test`).
- `application.properties` — `app.security.jwt.secret=${JWT_SECRET}`, `app.security.jwt.expiration-seconds`, `app.security.protect-subtareas=${PROTECT_SUBTAREAS:false}` y `app.security.legacy-user-id=${LEGACY_USER_ID:1}`.
- `src/test/resources/application-test.properties` — secreto de pruebas y `protect-subtareas=false` por defecto.
- `.env.example` — `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `PROTECT_SUBTAREAS`, `LEGACY_USER_ID`.
- `render.yaml` — `JWT_SECRET: sync: false`, `JWT_EXPIRATION_SECONDS=28800`, `PROTECT_SUBTAREAS=false`, `LEGACY_USER_ID=1`.

### Backend — seguridad

- `security/JwtService.java` — emisión de tokens (subject = `idUsuario`), `claveDesdeSecreto` con HS256.
- `security/JwtAuthenticationFilter.java` — lee `Authorization: Bearer`, valida y publica la identidad. Token ausente no es error; token inválido se descarta y la ruta protegida responde 401.
- `security/CurrentUserProvider.java` — propietario efectivo; con la bandera activa se niega a caer al usuario legado.
- `security/RestAuthenticationEntryPoint.java` — 401 en `application/problem+json`.
- `config/SecurityConfig.java` — cadena stateless, CORS integrado, matcher condicional de subtareas y perfil protegido. Usa `AuthenticationTrustResolverImpl` porque `AnonymousAuthenticationToken.isAuthenticated()` es `true` y un `&&` a secas dejaría pasar justo las peticiones sin token.
- `config/CorsConfig.java` — `CorsConfigurationSource` limitado a `/api/**`, en el mismo lugar que el CORS de MVC.
- `config/OpenApiConfig.java` — esquema `bearerAuth` para el botón **Authorize**.

### Backend — autenticación de usuarios

- `model/dto/RegistroRequestDTO.java`, `LoginRequestDTO.java`, `AuthResponseDTO.java` — contrato de entrada/salida.
- `service/AuthService.java` — registro, login y perfil; BCrypt strength 10, emails a minúsculas y mismo 401 para usuario inexistente y contraseña incorrecta (no enumerar cuentas).
- `controller/UsuarioController.java` — `register`, `login` y `profile`, cada uno con su variante con barra final.
- `exception/` — `SinAutenticacionException`, `CredencialesInvalidasException`, `EmailDuplicadoException` y sus handlers 401/409.

### Backend — aislamiento (el cierre del IDOR)

- `repository/EventoRepository.java` — deja de extender `JpaRepository`; expone `findByUsuarioId`, `findByIdYUsuarioId`, `save`, `delete`, `flush`.
- `repository/SubtareaRepository.java` — `findByEventoIdYUsuarioId` y `findByIdYUsuarioId`; sin `findAll()` ni `findById()`.
- `service/EventoService.java` — **todas** las operaciones reciben `usuarioId`: `obtenerTodos`, `obtenerPorId`, `crearEvento`, `actualizarEvento`, `eliminarEvento`. `crearEvento` ignora el `idUsuario` del cuerpo; `actualizarEvento` ya no reasigna el propietario (antes podía robarse el evento de otro).
- `service/SubtareaService.java` — `buscarEvento` usa `findByIdYUsuarioId` en vez de `findById` + comparación en Java.
- `controller/EventoController.java` — pasa `propietario()` en las cinco rutas; el `usuarioId` del query de listado se acepta pero se ignora.

### Frontend

- `src/services/tokenStorage.js` (**nuevo**) — persistencia y evento de sesión expirada. Vive aparte a propósito: `authService` importa de `api.js` y si `api.js` importara de `authService` se formaría un ciclo.
- `src/services/authService.js` — se elimina el login simulado `/api/auth/login`; ahora llama a `/api/users/login` y `/api/users/register`, guarda la sesión y traduce los `ProblemDetail` del backend a mensajes en español.
- `src/services/api.js` — `getAuthToken()` pasa a leer el token real y a adjuntar `Authorization: Bearer`; un 401 fuera del login dispara `notifySessionExpired()`. Nuevo `api.getProfile()`.
- `src/providers/SessionProvider.jsx` + `session-context.js` (**nuevos**) — `login`, `register`, `logout`, `usuario`; al montar, si hay token sin datos de usuario, consulta el perfil.
- `src/views/LoginView.jsx` — se agrega contraseña, mostrar/ocultar, error 401 y enlace a registro, conservando el diseño existente.
- `src/views/RegisterView.jsx` + `src/pages/RegisterPage.jsx` (**nuevos**) — alta de cuenta con las mismas reglas de contraseña del backend.
- `src/routes/AppRoutes.jsx` — `RequireSession` protege el panel; `RedirectIfAuthenticated` saca de `/login` y `/registro` a quien ya tiene sesión. La ruta `*` ahora cae en `/hoy` (antes mandaba a `/login`).
- `src/components/ui/Layout.jsx` — iniciales del usuario y botón **Cerrar sesión**.
- `src/utils/passwordValidation.js` (**nuevo**) — reglas de contraseña (8–72, al menos una letra y un número) replicando `RegistroRequestDTO` para no hacer un viaje de red por cada tecla.
- `vite.config.js` — `test.environment: 'jsdom'`; sin DOM los tests de `localStorage` no corren (se terciaban en `node`).
- `package.json` — `jsdom` como devDependency.

### Documentación

- `README.md` — variables nuevas, endpoints de auth, ejemplo de respuesta, sección de aislamiento por propietario, tabla de activación gradual y estado de la hoja de ruta.
- `AGENTS.md` — sección `Security (US-11, JWT)` con las reglas que no se deben romper.

## Verificación

- `./mvnw test` — **141 pruebas**, 0 fallos, 0 errores (antes 130). Incluye `JwtSecurityIntegrationTest` (con `protect-subtareas=true`), `EventoRepositoryAislamientoTest` y `SubtareaRepositoryAislamientoTest`.
- `npm test` (frontend) — **52 pruebas**, 0 fallos; cubren `tokenStorage`, `authService`, el `Authorization` en `api.js` y las reglas de contraseña.
- `npm run lint` (oxlint) — sin avisos.
- `npm run build` — compila; `RegisterPage` sale en su propio chunk por el `lazy` de rutas.

### Pruebas que fijan el contrato nuevo

- `listarEventosDevuelveSoloLosDelToken` — el `usuarioId` del query se ignora.
- `crearEventoAsignaElPropietarioDelToken` — el cuerpo pide otro usuario y el evento sigue naciendo a nombre de quien llama.
- `actualizarUnEventoAjenoResponde404` / `eliminarUnEventoAjenoResponde404` — y el evento sobrevive intacto.
- `crearEventoIgnoraElUsuarioDelCuerpoYAsignaElPropietarioDeLaSesion` — reemplazo de la antigua prueba que esperaba 404 por un `idUsuario` inexistente, comportamiento que ya no existe.

## Pendiente

- Configurar `JWT_SECRET` en el panel de Render. **Sin él el servicio no arranca.**
- Desplegar el frontend y luego subir `PROTECT_SUBTAREAS=true`.
- Los usuarios preexistentes en Supabase pueden tener `password_hash` que no corresponde a una contraseña conocida: la vía es registrar una cuenta nueva.
