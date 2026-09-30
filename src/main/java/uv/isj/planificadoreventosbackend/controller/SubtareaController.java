package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
            @ApiResponse(responseCode = "400", description = "La fecha o el organizador no son válidos")
    })
    public List<SubtareaDTO> listarParaHoy(
            @Parameter(description = "Identificador del organizador", example = "1")
            @RequestParam Integer usuarioId,
            @Parameter(description = "Fecha objetivo en formato ISO", example = "2026-09-25")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return subtareaService.obtenerParaHoy(propietarioDeHoy(usuarioId), fecha);
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
    @Operation(summary = "Reprogramar una subtarea y evaluar el límite diario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reprogramación evaluada; puede indicar conflicto"),
            @ApiResponse(responseCode = "400", description = "La nueva fecha o las horas no son válidas"),
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    public ResponseEntity<Map<String, Object>> reprogramar(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ReprogramarDTO dto) {
        Integer usuarioId = propietario();
        Integer limiteDiario = subtareaService.obtenerLimiteDiario(usuarioId, id);
        Map<String, Object> resultado =
                subtareaService.reprogramar(usuarioId, id, dto, limiteDiario.doubleValue());
        return ResponseEntity.ok(resultado);
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
            @ApiResponse(responseCode = "404", description = "La subtarea no existe")
    })
    public SubtareaDTO cambiarEstado(
            @Parameter(description = "Identificador de la subtarea", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody EstadoSubtareaDTO dto) {
        return subtareaService.cambiarEstado(propietario(), id, dto);
    }

    /**
     * Con token manda el del token y el query param se ignora. Sin token se
     * respeta el parametro, que sigue siendo obligatorio para conservar el 400
     * documentado en ParametrosApiTest.
     */
    private Integer propietarioDeHoy(Integer usuarioIdSolicitado) {
        return currentUserProvider.esAutenticado()
                ? currentUserProvider.idUsuarioRequerido()
                : usuarioIdSolicitado;
    }
}
