package uv.isj.planificadoreventosbackend.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.service.EventoService;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@RestController
@RequestMapping("/api/subtareas")
@Transactional
public class SubtareaController {

    private final SubtareaService subtareaService;
    private final EventoService eventoService;

    public SubtareaController(SubtareaService subtareaService, EventoService eventoService) {
        this.subtareaService = subtareaService;
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<SubtareaDTO> listarPorEvento(@RequestParam Integer eventoId) {
        return subtareaService.findByEvento(eventoId).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public SubtareaDTO obtener(@PathVariable Integer id) {
        return toDto(buscar(id));
    }

    @PostMapping
    public ResponseEntity<SubtareaDTO> crear(@Valid @RequestBody SubtareaDTO dto) {
        Subtarea subtarea = new Subtarea();
        aplicar(subtarea, dto);
        Subtarea guardada = subtareaService.save(subtarea);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(guardada));
    }

    @PutMapping("/{id}")
    public SubtareaDTO actualizar(@PathVariable Integer id, @Valid @RequestBody SubtareaDTO dto) {
        Subtarea subtarea = buscar(id);
        aplicar(subtarea, dto);
        return toDto(subtareaService.save(subtarea));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        subtareaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Subtarea buscar(Integer id) {
        return subtareaService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una subtarea con el id " + id));
    }

    private void aplicar(Subtarea subtarea, SubtareaDTO dto) {
        Evento evento = eventoService.findById(dto.idEvento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un evento con el id " + dto.idEvento()));

        subtarea.setEvento(evento);
        subtarea.setNombreGestion(dto.nombreGestion().trim());
        subtarea.setFechaObjetivo(dto.fechaObjetivo());
        subtarea.setHorasEstimadas(dto.horasEstimadas());
        subtarea.setEstado(dto.estado() == null ? EstadoSubtarea.pendiente : dto.estado());
        subtarea.setNotaExplicativa(dto.notaExplicativa());
    }

    private SubtareaDTO toDto(Subtarea subtarea) {
        return new SubtareaDTO(
                subtarea.getIdSubtarea(),
                subtarea.getEvento().getIdEvento(),
                subtarea.getNombreGestion(),
                subtarea.getFechaObjetivo(),
                subtarea.getHorasEstimadas(),
                subtarea.getEstado(),
                subtarea.getNotaExplicativa(),
                subtarea.getFechaCreacion());
    }
}
