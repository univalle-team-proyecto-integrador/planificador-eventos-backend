package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final SubtareaRepository subtareaRepository;

    public EventoService(
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            TipoEventoRepository tipoEventoRepository,
            SubtareaRepository subtareaRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.subtareaRepository = subtareaRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerTodos(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El organizador es obligatorio");
        }

        return eventoRepository.findByUsuarioId(usuarioId).stream()
                .map(evento -> aDto(evento, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoDTO obtenerPorId(Integer usuarioId, Integer id) {
        Evento evento = buscarEntidadDeUsuario(usuarioId, id);
        List<SubtareaDTO> subtareas = subtareaRepository
                .findByEventoIdYUsuarioId(id, usuarioId).stream()
                .map(this::aSubtareaDto)
                .toList();
        return aDto(evento, subtareas);
    }

    @Transactional
    public EventoDTO actualizarEvento(Integer usuarioId, Integer id, EventoDTO dto) {
        validar(dto);
        Evento evento = buscarEntidadDeUsuario(usuarioId, id);
        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.idTipoEvento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el tipo de evento con id " + dto.idTipoEvento()));

        // El propietario no se reasigna: el idUsuario del cuerpo se ignora para
        // que una cuenta no robe el evento de otra.
        evento.setTipoEvento(tipoEvento);
        evento.setNombre(dto.nombre().trim());
        evento.setCliente(dto.cliente().trim());
        evento.setFechaEvento(dto.fechaEvento());
        evento.setLugar(dto.lugar().trim());
        return aDto(eventoRepository.save(evento), List.of());
    }

    @Transactional
    public EventoDTO crearEvento(Integer usuarioId, EventoDTO dto) {
        validar(dto);

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El organizador es obligatorio");
        }

        // El evento siempre nace a nombre de quien llama: el idUsuario del
        // cuerpo se acepta por compatibilidad pero no decide el propietario.
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + usuarioId));
        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.idTipoEvento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el tipo de evento con id " + dto.idTipoEvento()));

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipoEvento);
        evento.setNombre(dto.nombre().trim());
        evento.setCliente(dto.cliente().trim());
        evento.setFechaEvento(dto.fechaEvento());
        evento.setLugar(dto.lugar().trim());

        return aDto(eventoRepository.save(evento), List.of());
    }

    @Transactional
    public void eliminarEvento(Integer usuarioId, Integer id) {
        Evento evento = buscarEntidadDeUsuario(usuarioId, id);
        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    /**
     * Un evento ajeno se reporta como inexistente, no como prohibido: un 403
     * confirmaría que el id existe en la cuenta de otro usuario.
     */
    private Evento buscarEntidadDeUsuario(Integer usuarioId, Integer id) {
        return eventoRepository.findByIdYUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el evento con id " + id));
    }

    private void validar(EventoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del evento son obligatorios");
        }
        if (dto.nombre() == null || dto.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del evento es obligatorio");
        }
        if (dto.idTipoEvento() == null) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        if (dto.cliente() == null || dto.cliente().isBlank()) {
            throw new IllegalArgumentException("El cliente del evento es obligatorio");
        }
        if (dto.fechaEvento() == null) {
            throw new IllegalArgumentException("La fecha del evento es obligatoria");
        }
        if (dto.lugar() == null || dto.lugar().isBlank()) {
            throw new IllegalArgumentException("El lugar del evento es obligatorio");
        }
    }

    private EventoDTO aDto(Evento evento, List<SubtareaDTO> subtareas) {
        return new EventoDTO(
                evento.getIdEvento(),
                evento.getUsuario().getIdUsuario(),
                evento.getTipoEvento().getIdTipoEvento(),
                evento.getNombre(),
                evento.getCliente(),
                evento.getFechaEvento(),
                evento.getLugar(),
                evento.getFechaCreacion(),
                subtareas);
    }

    private SubtareaDTO aSubtareaDto(Subtarea subtarea) {
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
