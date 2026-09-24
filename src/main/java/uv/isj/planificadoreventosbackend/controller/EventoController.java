package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.service.EventoService;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Gestión de eventos y sus subtareas")
public class EventoController {

    private final EventoService eventoService;
    private final SubtareaService subtareaService;

    public EventoController(EventoService eventoService, SubtareaService subtareaService) {
        this.eventoService = eventoService;
        this.subtareaService = subtareaService;
    }

    @GetMapping
    @Operation(summary = "Listar eventos")
    @ApiResponse(responseCode = "200", description = "Lista de eventos")
    public List<EventoDTO> obtenerTodos(
            @Parameter(description = "Filtra por organizador", example = "1")
            @RequestParam(required = false) Integer usuarioId) {
        return eventoService.obtenerTodos(usuarioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del evento"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public EventoDTO obtenerPorId(
            @Parameter(description = "Identificador del evento", example = "1")
            @PathVariable Integer id) {
        return eventoService.obtenerPorId(id);
    }

    @PostMapping
    @Operation(summary = "Crear un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado"),
            @ApiResponse(responseCode = "400", description = "Los datos del evento no son válidos"),
            @ApiResponse(responseCode = "404", description = "El usuario o el tipo de evento no existe")
    })
    public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoDTO dto) {
        EventoDTO evento = eventoService.crearEvento(dto);
        URI ubicacion = URI.create("/api/eventos/" + evento.idEvento());
        return ResponseEntity.created(ubicacion).body(evento);
    }

    @GetMapping("/{id}/subtareas")
    @Operation(summary = "Listar subtareas de un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtareas del evento"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public List<SubtareaDTO> obtenerSubtareas(
            @Parameter(description = "Identificador del evento", example = "1")
            @PathVariable Integer id) {
        return subtareaService.obtenerPorEvento(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento actualizado"),
            @ApiResponse(responseCode = "400", description = "Los datos no son válidos"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public EventoDTO actualizar(
            @Parameter(description = "Identificador del evento", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody EventoDTO dto) {
        return eventoService.actualizarEvento(id, dto);
    }

    @PostMapping("/{id}/subtareas")
    @Operation(summary = "Agregar una subtarea a un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subtarea creada con estado pendiente"),
            @ApiResponse(responseCode = "400", description = "Los datos de la subtarea no son válidos"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public ResponseEntity<SubtareaDTO> agregarSubtarea(
            @Parameter(description = "Identificador del evento", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody SubtareaDTO dto) {
        SubtareaDTO subtarea = subtareaService.agregarSubtarea(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(subtarea);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un evento y sus subtareas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evento y subtareas eliminados"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del evento", example = "1")
            @PathVariable Integer id) {
        eventoService.eliminarEvento(id);
        return ResponseEntity.noContent().build();
    }
}
