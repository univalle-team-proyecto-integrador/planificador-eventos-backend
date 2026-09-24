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
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.service.EventoService;
import uv.isj.planificadoreventosbackend.service.TipoEventoService;
import uv.isj.planificadoreventosbackend.service.UsuarioService;

@RestController
@RequestMapping("/api/eventos")
@Transactional
public class EventoController {

    private final EventoService eventoService;
    private final UsuarioService usuarioService;
    private final TipoEventoService tipoEventoService;

    public EventoController(
            EventoService eventoService,
            UsuarioService usuarioService,
            TipoEventoService tipoEventoService) {
        this.eventoService = eventoService;
        this.usuarioService = usuarioService;
        this.tipoEventoService = tipoEventoService;
    }

    @GetMapping
    public List<EventoDTO> listar(@RequestParam(required = false) Integer usuarioId) {
        List<Evento> eventos = usuarioId == null
                ? eventoService.findAll()
                : eventoService.findByUsuario(usuarioId);
        return eventos.stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public EventoDTO obtener(@PathVariable Integer id) {
        return toDto(buscar(id));
    }

    @PostMapping
    public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoDTO dto) {
        Evento evento = new Evento();
        aplicar(evento, dto);
        Evento guardado = eventoService.save(evento);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(guardado));
    }

    @PutMapping("/{id}")
    public EventoDTO actualizar(@PathVariable Integer id, @Valid @RequestBody EventoDTO dto) {
        Evento evento = buscar(id);
        aplicar(evento, dto);
        return toDto(eventoService.save(evento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        eventoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Evento buscar(Integer id) {
        return eventoService.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un evento con el id " + id));
    }

    private void aplicar(Evento evento, EventoDTO dto) {
        Usuario usuario = usuarioService.findById(dto.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con el id " + dto.idUsuario()));
        TipoEvento tipoEvento = tipoEventoService.findById(dto.idTipoEvento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un tipo de evento con el id " + dto.idTipoEvento()));

        evento.setUsuario(usuario);
        evento.setTipoEvento(tipoEvento);
        evento.setNombre(dto.nombre().trim());
        evento.setCliente(dto.cliente().trim());
        evento.setFechaEvento(dto.fechaEvento());
        evento.setLugar(dto.lugar().trim());
    }

    private EventoDTO toDto(Evento evento) {
        return new EventoDTO(
                evento.getIdEvento(),
                evento.getUsuario().getIdUsuario(),
                evento.getTipoEvento().getIdTipoEvento(),
                evento.getNombre(),
                evento.getCliente(),
                evento.getFechaEvento(),
                evento.getLugar(),
                evento.getFechaCreacion());
    }
}
