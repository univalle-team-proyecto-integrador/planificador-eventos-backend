package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.EstadoSubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@RestController
@RequestMapping("/api/subtareas")
@Tag(name = "Subtareas", description = "Reprogramación y cambio de estado de subtareas")
public class SubtareaController {

    private final SubtareaService subtareaService;

    public SubtareaController(SubtareaService subtareaService) {
        this.subtareaService = subtareaService;
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

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Marcar una subtarea como ejecutada o pospuesta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
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
