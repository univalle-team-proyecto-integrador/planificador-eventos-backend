package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.EstadoSubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaActualizacionDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.HoyResponseDTO;
import uv.isj.planificadoreventosbackend.security.CurrentUserProvider;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@RestController
@RequestMapping("/api/subtareas")
@Tag(name = "Subtareas", description = "Gestión, reprogramación y cambio de estado de subtareas")
public class SubtareaController {

    private final SubtareaService subtareaService;
    private final CurrentUserProvider currentUserProvider;

    public SubtareaController(
            SubtareaService subtareaService,
            CurrentUserProvider currentUserProvider) {
        this.subtareaService = subtareaService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Propietario efectivo de la petición. Si hay token manda el del token y se
     * ignora el query param, de modo que pedir los datos de otra cuenta
     * cambiando la URL no da resultado.
     */
    private Integer propietario() {
        return currentUserProvider.idUsuarioActual();
    }

    @GetMapping("/hoy")
    @Operation(summary = "Listar las gestiones no ejecutadas de hoy")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gestiones no ejecutadas para la fecha consultada"),
            @ApiResponse(responseCode = "401",
                    description = "Falta un token de autenticación válido o el enviado no es válido")
    })
    @SecurityRequirement(name = "bearerAuth")
    public List<SubtareaDTO> listarParaHoy(
            @Parameter(description = "Identificador del organizador. Opcional y solo se "
                    + "honra en peticiones anónimas, por compatibilidad con el cliente anterior "
                    + "a la autenticación; con token manda el del token",
                    example = "1")
            @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Fecha objetivo en formato ISO", example = "2026-09-25")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return subtareaService.obtenerParaHoy(propietarioDeHoy(usuarioId), fecha);
    }

    @GetMapping("/hoy/agrupado")
    @Operation(
            summary = "Listar las gestiones no ejecutadas de hoy agrupadas por vencidas/para hoy/próximas",
            description = "Devuelve las subtareas no ejecutadas del usuario autenticado agrupadas por "
                    + "fecha objetivo (< hoy: vencidas; == hoy: paraHoy; > hoy: proximas). "
                    + "Soporta filtros opcionales por nombre del evento y estado. "
                    + "Requiere autorización Bearer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Gestiones no ejecutadas agrupadas por fecha objetivo"),
            @ApiResponse(responseCode = "401",
                    description = "Falta un token de autenticación válido o el enviado no es válido")
    })
    @SecurityRequirement(name = "bearerAuth")
    public HoyResponseDTO listarParaHoyAgrupado(
            @Parameter(description = "Identificador del organizador. Opcional y solo se "
                    + "honra en peticiones anónimas, por compatibilidad; con token manda el del token",
                    example = "1")
            @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Fecha objetivo en formato ISO (yyyy-MM-dd)", example = "2026-10-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return subtareaService.obtenerHoyAgrupado(propietarioDeHoy(usuarioId), fecha);
    }

    @GetMapping
    @Operation(summary = "Listar subtareas de un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de subtareas"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public List<SubtareaDTO> listarPorEvento(
            @Parameter(description = "Identificador del evento", example = "1")
            @RequestParam Integer eventoId) {
        return subtareaService.obtenerPorEvento(propietario(), eventoId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar los datos editables de una subtarea")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtarea actualizada"),
            @ApiResponse(responseCode = "400", description = "Los datos no son válidos"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    public SubtareaDTO actualizar(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody SubtareaActualizacionDTO dto) {
        return subtareaService.actualizarSubtarea(propietario(), id, dto);
    }

    @PatchMapping("/{id}/reprogramar")
    @Operation(
            summary = "Reprogramar una subtarea respetando el límite diario",
            description = "Devuelve 200 con la subtarea actualizada cuando cabe en el límite. "
                    + "Si lo supera devuelve 409 sin guardar nada, con el detalle de cuántas "
                    + "horas hay que liberar. "
                    + "Requiere autorización Bearer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reprogramación aplicada"),
            @ApiResponse(responseCode = "400", description = "La nueva fecha o las horas no son válidas"),
            @ApiResponse(responseCode = "401",
                    description = "Falta un token de autenticación válido o el enviado no es válido"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe"),
            @ApiResponse(responseCode = "409", description = "Supera el límite diario; la subtarea no se modificó")
    })
    @SecurityRequirement(name = "bearerAuth")
    public SubtareaDTO reprogramar(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ReprogramarDTO dto) {
        return subtareaService.reprogramar(propietario(), id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una subtarea")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la subtarea"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    public SubtareaDTO obtenerPorId(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id) {
        return subtareaService.obtenerPorId(propietario(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una subtarea")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subtarea eliminada"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id) {
        subtareaService.eliminarSubtarea(propietario(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado de una subtarea")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado; una subtarea puede reabrirse a pendiente"),
            @ApiResponse(responseCode = "400", description = "El estado solicitado no es válido"),
            @ApiResponse(responseCode = "401",
                    description = "Falta un token de autenticación válido o el enviado no es válido"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    @SecurityRequirement(name = "bearerAuth")
    public SubtareaDTO cambiarEstado(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody EstadoSubtareaDTO dto) {
        return subtareaService.cambiarEstado(propietario(), id, dto);
    }

    /**
     * Propietario efectivo de "hoy". Con token manda el del token; sin token se
     * acepta el {@code usuarioId} consultado por compatibilidad, y si tampoco
     * viene se cae al usuario legado. Nunca se propaga un {@code null} al
     * repositorio, y con {@code protect-subtareas} activo ambos caminos exigen
     * token a través de {@link CurrentUserProvider}.
     */
    private Integer propietarioDeHoy(Integer usuarioIdSolicitado) {
        if (currentUserProvider.esAutenticado()) {
            return currentUserProvider.idUsuarioRequerido();
        }
        return usuarioIdSolicitado != null ? usuarioIdSolicitado : currentUserProvider.idUsuarioActual();
    }
}
