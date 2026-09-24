package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.EstadoSubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@RestController
@RequestMapping("/api/subtareas")
@Tag(name = "Subtareas", description = "Gestión, reprogramación y cambio de estado de subtareas")
public class SubtareaController {

    private final SubtareaService subtareaService;

    public SubtareaController(SubtareaService subtareaService) {
        this.subtareaService = subtareaService;
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
        return subtareaService.obtenerPorEvento(eventoId);
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
        Integer limiteDiario = subtareaService.obtenerLimiteDiario(id);
        Map<String, Object> resultado =
                subtareaService.reprogramar(id, dto, limiteDiario.doubleValue());
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
        return subtareaService.obtenerPorId(id);
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
        subtareaService.eliminarSubtarea(id);
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
        return subtareaService.cambiarEstado(id, dto);
    }
}
