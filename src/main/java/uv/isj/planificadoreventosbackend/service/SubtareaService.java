package uv.isj.planificadoreventosbackend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.CapacidadExcedidaException;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EstadoSubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaActualizacionDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;

@Service
public class SubtareaService {

    private final SubtareaRepository subtareaRepository;
    private final EventoRepository eventoRepository;

    public SubtareaService(
            SubtareaRepository subtareaRepository,
            EventoRepository eventoRepository) {
        this.subtareaRepository = subtareaRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<SubtareaDTO> obtenerPorEvento(Integer eventoId) {
        buscarEvento(eventoId);
        return subtareaRepository.findByEventoId(eventoId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubtareaDTO> obtenerParaHoy(Integer usuarioId, LocalDate fecha) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El organizador es obligatorio");
        }

        LocalDate fechaConsulta = fecha == null
                ? LocalDate.now(ZoneId.of("America/Bogota"))
                : fecha;
        return subtareaRepository
                .findNoEjecutadasParaHoy(
                        usuarioId,
                        fechaConsulta,
                        EstadoSubtarea.ejecutada)
                .stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubtareaDTO obtenerPorId(Integer id) {
        return aDto(buscarEntidad(id));
    }

    @Transactional
    public SubtareaDTO agregarSubtarea(Integer eventoId, SubtareaDTO dto) {
        validarSubtarea(eventoId, dto);
        Evento evento = buscarEvento(eventoId);

        Subtarea subtarea = new Subtarea();
        evento.agregarSubtarea(subtarea);
        subtarea.setNombreGestion(dto.nombreGestion().trim());
        subtarea.setFechaObjetivo(dto.fechaObjetivo());
        subtarea.setHorasEstimadas(dto.horasEstimadas());
        subtarea.setEstado(EstadoSubtarea.pendiente);
        subtarea.setNotaExplicativa(dto.notaExplicativa());

        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public SubtareaDTO actualizarSubtarea(
            Integer id,
            SubtareaActualizacionDTO dto) {
        validarActualizacion(dto);
        Subtarea subtarea = buscarEntidad(id);
        subtarea.setNombreGestion(dto.nombreGestion().trim());
        subtarea.setFechaObjetivo(dto.fechaObjetivo());
        subtarea.setHorasEstimadas(dto.horasEstimadas());
        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public SubtareaDTO reprogramar(Integer id, ReprogramarDTO dto) {
        Subtarea subtarea = buscarEntidad(id);
        validarReprogramacion(dto);

        Evento evento = subtarea.getEvento();
        Usuario usuario = evento.getUsuario();
        int limiteDiario = usuario.getLimiteHorasDiarias();
        validarLimiteDiario(limiteDiario);

        int horasExistentes = (int) subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                usuario.getIdUsuario(),
                dto.nuevaFecha(),
                EstadoSubtarea.ejecutada);

        if (subtarea.getFechaObjetivo().equals(dto.nuevaFecha())
                && subtarea.getEstado() != EstadoSubtarea.ejecutada) {
            horasExistentes = Math.max(0, horasExistentes - subtarea.getHorasEstimadas());
        }

        int horasPlanificadasTotales = horasExistentes + dto.nuevasHoras();
        if (horasPlanificadasTotales > limiteDiario) {
            throw new CapacidadExcedidaException(
                    subtarea.getIdSubtarea(),
                    dto.nuevaFecha(),
                    limiteDiario,
                    horasExistentes,
                    dto.nuevasHoras(),
                    horasPlanificadasTotales);
        }

        subtarea.setFechaObjetivo(dto.nuevaFecha());
        subtarea.setHorasEstimadas(dto.nuevasHoras());
        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public SubtareaDTO cambiarEstado(Integer id, EstadoSubtareaDTO dto) {
        if (dto == null || dto.estado() == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }

        Subtarea subtarea = buscarEntidad(id);
        subtarea.setEstado(dto.estado());
        subtarea.setNotaExplicativa(dto.notaExplicativa());
        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public void eliminarSubtarea(Integer id) {
        Subtarea subtarea = buscarEntidad(id);
        Evento evento = subtarea.getEvento();
        evento.getSubtareas().remove(subtarea);
        subtareaRepository.delete(subtarea);
        subtareaRepository.flush();
    }

    private Evento buscarEvento(Integer eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el evento con id " + eventoId));
    }

    private Subtarea buscarEntidad(Integer id) {
        return subtareaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la subtarea con id " + id));
    }

    private void validarActualizacion(SubtareaActualizacionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la subtarea son obligatorios");
        }
        if (dto.nombreGestion() == null || dto.nombreGestion().isBlank()) {
            throw new IllegalArgumentException("El nombre de la subtarea es obligatorio");
        }
        if (dto.fechaObjetivo() == null) {
            throw new IllegalArgumentException("La fecha objetivo es obligatoria");
        }
        if (dto.horasEstimadas() == null || dto.horasEstimadas() <= 0) {
            throw new IllegalArgumentException("Las horas estimadas deben ser mayores que cero");
        }
    }

    private void validarSubtarea(Integer eventoId, SubtareaDTO dto) {
        if (eventoId == null) {
            throw new IllegalArgumentException("El id del evento es obligatorio");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la subtarea son obligatorios");
        }
        if (dto.nombreGestion() == null || dto.nombreGestion().isBlank()) {
            throw new IllegalArgumentException("El nombre de la subtarea es obligatorio");
        }
        if (dto.fechaObjetivo() == null) {
            throw new IllegalArgumentException("La fecha objetivo es obligatoria");
        }
        if (dto.horasEstimadas() == null || dto.horasEstimadas() <= 0) {
            throw new IllegalArgumentException("Las horas estimadas deben ser mayores que cero");
        }
    }

    private void validarReprogramacion(ReprogramarDTO dto) {
        if (dto == null || dto.nuevaFecha() == null) {
            throw new IllegalArgumentException("La nueva fecha es obligatoria");
        }
        if (dto.nuevasHoras() == null || dto.nuevasHoras() <= 0) {
            throw new IllegalArgumentException("Las nuevas horas deben ser mayores que cero");
        }
    }

    private void validarLimiteDiario(int limiteDiario) {
        if (limiteDiario < UsuarioService.LIMITE_MINIMO_HORAS
                || limiteDiario > UsuarioService.LIMITE_MAXIMO_HORAS) {
            throw new IllegalStateException(
                    "El usuario " + limiteDiario
                            + " tiene un límite de horas fuera del rango permitido");
        }
    }

    private SubtareaDTO aDto(Subtarea subtarea) {
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
