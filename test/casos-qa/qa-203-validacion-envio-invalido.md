---
tipo: caso-qa
area: api
estado: PASSED
fecha: 2026-09-25
---

# QA-203: Validación (API): Envío inválido

**ID Caso:** #203
**Tarea Relacionada:** Validación (API): Envío inválido
**Descripción / Escenario:** Asegurar que el Backend rechaza datos incorrectos.
**Pasos a Reproducir:**
1. En Postman, `POST /api/eventos/{idEvento}/subtareas` (endpoint de creación; equivale a `POST /api/activities/` del caso original; los campos se ajustan al DTO real: `nombreGestion` ← `title`, `horasEstimadas` ← `estimated_hours`).
2. Body (JSON):
   ```json
   {
     "idEvento": 9,
     "nombreGestion": "",
     "fechaObjetivo": "2026-11-10",
     "horasEstimadas": -5,
     "estado": "pendiente"
   }
   ```
3. Enviar.
4. Verificar `400 Bad Request` con detalle de errores y que no exista registro nuevo en BD (comprobar con `GET /api/subtareas?eventoId={idEvento}` antes y después).

**Resultado Esperado:** El POST inválido devuelve `400 Bad Request` con body de error (ProblemDetail) y el campo `errors` especificando cada problema; no se crea registro en BD.

**Resultado Obtenido:** Rechaza la petición con `400` y especifica los campos necesarios para realizar este POST:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "status": 400,
  "title": "Datos inválidos",
  "detail": "Uno o más campos enviados no son válidos",
  "errors": {
    "nombreGestion": "no debe estar vacío",
    "horasEstimadas": "debe ser mayor o igual a 1"
  }
}
```
La lista de subtareas del evento no cambia (no se creó el registro).

**Estado:** PASSED

---

**Notas técnicas (evidencia interna):**
- Validaciones en `SubtareaDTO.java`: `@NotBlank` en `nombreGestion`, `@Min(1)` en `horasEstimadas`.
- El `400` lo arma `GlobalExceptionHandler.handleValidacion` (ProblemDetail + `errors` por campo).
- Variantes equivalentes con `horasEstimadas`: `0`, `-2`, `"abc"` y `null` → `400` (SCRUM-8-QA-1; ver colección `test/postman/`).