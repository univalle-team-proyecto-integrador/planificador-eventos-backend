package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoEventoRepository tipoEventoRepository;

    public EventoService(
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            TipoEventoRepository tipoEventoRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoEventoRepository = tipoEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerTodos() {
        return eventoRepository.findAll().stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoDTO obtenerPorId(Integer id) {
        return aDto(buscarEntidad(id));
    }

    @Transactional
    public EventoDTO crearEvento(EventoDTO dto) {
        validar(dto);

        Usuario usuario = usuarioRepository.findById(dto.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + dto.idUsuario()));
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

        return aDto(eventoRepository.save(evento));
    }

    @Transactional
    public void eliminarEvento(Integer id) {
        Evento evento = buscarEntidad(id);
        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    private Evento buscarEntidad(Integer id) {
        return eventoRepository.findById(id)
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
        if (dto.idUsuario() == null) {
            throw new IllegalArgumentException("El usuario del evento es obligatorio");
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

    private EventoDTO aDto(Evento evento) {
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
