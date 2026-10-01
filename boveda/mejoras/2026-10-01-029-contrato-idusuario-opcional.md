---
tipo: mejora
---

# idUsuario y usuarioId dejan de ser obligatorios: el contrato coincide con el aislamiento

- **Fecha:** 2026-10-01
- **Área:** API, pruebas, documentación
- **Estado:** hecha

## Descripción

US-11 dejó el aislamiento por propietario resuelto, pero el contrato seguía mintiendo. `POST /api/eventos` exigía `idUsuario` con `@NotNull` y el cliente lo mandaba siempre, cuando el valor real lo decide el token. `GET /api/subtareas/hoy` exigía `usuarioId` en el query por la misma razón.

El resultado era un contrato que pedía un dato para después descartarlo. Peor: obligaba al cliente a tener un concepto de "usuario actual" que ya no existe, y cualquier valor equivocado se aceptaba igual. Un campo obligatorio que se ignora no es validación, es ruido con apariencia de seguridad.

## Cambios

Backend:

- `model/dto/EventoDTO.java` — `idUsuario` deja de ser `@NotNull` y su `Schema` aclara que se ignora.
- `service/EventoService.java` — se quita la validación que rechazaba `idUsuario` nulo. Sigue sin aceptarse un propietario desde el cuerpo: el del token.
- `controller/SubtareaController.java` — `usuarioId` pasa a `required = false`, con la regla de precedencia escrita en el `Schema` y en el javadoc de `propietarioDeHoy`.
- `scripts/verificar-despliegue.sh` — deja de enviar `usuarioId` a propósito, para que un despliegue viejo se note.
- `test/postman/...` — los cuerpos de evento ya no llevan `idUsuario`; se renombró la request de listado para dejar claro que el query param se ignora, y se añadió `/api/subtareas/hoy` sin `usuarioId`.

Frontend:

- `services/api.js` — `listEvents()` y `listTodaySubtasks(fecha)` ya no construyen query con `usuarioId`. Se elimina `getDefaultUserId`, que solo servía para ese efecto.
- `views/CreateEventView.jsx`, `views/EventDetailView.jsx` — sin `idUsuario` en los cuerpos de creación y actualización.
- `pages/HoyPage.jsx`, `pages/ProgresoPage.jsx` — llaman `listEvents()` sin argumentos.
- `.env.example`, `README.md`, `AGENTS.md` — `VITE_USER_ID` desaparece; era la fuente de ese id legado.

## Un detalle que no era trivial

Hacer `usuarioId` opcional en `/hoy` metía un `null` en el camino del propietario. Sin más cuidado, la petición anónima terminaba en una consulta sin dueño y devolvía una lista vacía: un 200 que parece "no tienes tareas" cuando en realidad es un parámetro que falta.

`propietarioDeHoy` resuelve la precedencia entera: con token manda el del token; sin token se acepta el `usuarioId` consultado por compatibilidad; sin ninguno de los dos se cae a `LEGACY_USER_ID`. Nunca llega un `null` al repositorio, y con `protect-subtareas` activo ambos caminos anónimos terminan en 401 a través de `CurrentUserProvider`.

## Verificación

- `./mvnw test` → **147 pruebas, 0 fallos** (eran 143).
- Pruebas nuevas: creación de evento sin `idUsuario` devuelve 201 con el propietario del token; `/hoy` sin `usuarioId` con token devuelve solo lo del token, no lo de la víctima.
- `ParametrosApiTest` fijaba el contrato viejo (400 por falta de `usuarioId`); se invirtió y se añadió `hoyAceptaUsuarioIdAusenteYTomaElPropietarioDelToken`.
- `EventoServiceTest` exigía que `idUsuario` nulo fuera un error. Se eliminó esa aserción y se añadió `crearEventoAceptaIdUsuarioNuloYUsaElPropietarioDeLaPeticion`.
- Newman contra la app local con H2 → 31 requests, 0 fallos.
- Comprobación manual con curl: `/api/subtareas/hoy?fecha=...` sin `usuarioId` → 200; `POST /api/eventos` sin `idUsuario` → 201 con `idUsuario: 7`, igual al del token.
- Frontend: `npm test` → 51 pruebas, `npm run lint` y `npm run build` limpios.

## Pendiente

Render sigue con `PROTECT_SUBTAREAS=false`. Ese es el último paso de US-11 y no se puede hacer desde el repositorio.
## Actualización 2026-10-01 (Tarea 4)

Se añade endpoint `GET /api/subtareas/hoy/agrupado` con respuesta:
```json
{
  "vencidas": [...],
  "paraHoy": [...],
  "proximas": [...]
}
```

- Servicio: `SubtareaService.obtenerHoyAgrupado` clasifica no ejecutadas (`estado != ejecutada`) comparando `fechaObjetivo` con `hoy` (antes/igual/después). Usa rango `fechaObjetivo <= hoy+30` para evitar traer todo sin límite.
- Repositorio: `findNoEjecutadasHastaFecha(usuarioId, hasta, estado)`.
- DTO: `HoyResponseDTO` con los tres grupos.
- Endpoint compatible: se mantiene `/api/subtareas/hoy` devolviendo lista plana; no se rompe el contrato existente.

## Actualización 2026-10-01 (Tarea 4 verificada en prod)

Endpoint `GET /api/subtareas/hoy/agrupado` verificado en producción: 200 con clasificación correcta (vencidas vacías, paraHoy con 3, próximas con 2). Mantiene `/api/subtareas/hoy` sin cambios.
