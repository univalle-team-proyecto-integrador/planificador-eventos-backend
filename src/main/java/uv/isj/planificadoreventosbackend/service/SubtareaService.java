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
import uv.isj.planificadoreventosbackend.model.dto.HoyResponseDTO;
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
    public List<SubtareaDTO> obtenerPorEvento(Integer usuarioId, Integer eventoId) {
        buscarEvento(usuarioId, eventoId);
        return subtareaRepository.findByEventoIdYUsuarioId(eventoId, usuarioId).stream()
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
    public SubtareaDTO obtenerPorId(Integer usuarioId, Integer id) {
        return aDto(buscarEntidad(usuarioId, id));
    }

    @Transactional
    public SubtareaDTO agregarSubtarea(Integer usuarioId, Integer eventoId, SubtareaDTO dto) {
        validarSubtarea(eventoId, dto);
        Evento evento = buscarEvento(usuarioId, eventoId);

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
            Integer usuarioId,
            Integer id,
            SubtareaActualizacionDTO dto) {
        validarActualizacion(dto);
        Subtarea subtarea = buscarEntidad(usuarioId, id);
        fijarLineaBaseSiFalta(subtarea, dto.fechaObjetivo());
        subtarea.setNombreGestion(dto.nombreGestion().trim());
        subtarea.setFechaObjetivo(dto.fechaObjetivo());
        subtarea.setHorasEstimadas(dto.horasEstimadas());
        return aDto(subtareaRepository.save(subtarea));
    }

    /**
     * Reprograma una subtarea respetando el límite diario del propietario.
     *
     * <p>El límite se resuelve aquí desde el usuario del evento en lugar de
     * recibirlo del controlador: así no hay dos llamadas al servicio ni una
     * ventana en la que el límite y la subtarea puedan no ser del mismo dueño.
     *
     * <p>Si el total supera el límite se lanza {@link CapacidadExcedidaException}
     * y no se guarda nada; el controlador la traduce a un 409. Si cabe, devuelve
     * la subtarea ya actualizada.
     */
    @Transactional
    public SubtareaDTO reprogramar(
            Integer usuarioId,
            Integer id,
            ReprogramarDTO dto) {
        Subtarea subtarea = buscarEntidad(usuarioId, id);
        validarReprogramacion(dto);

        Evento evento = subtarea.getEvento();
        Usuario usuario = evento.getUsuario();
        int limiteDiario = usuario.getLimiteHorasDiarias();
        validarLimiteDiario(limiteDiario);

        // La consulta ya excluye las ejecutadas: solo cuenta lo pendiente.
        int horasExistentes = (int) subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                usuario.getIdUsuario(),
                dto.nuevaFecha(),
                EstadoSubtarea.ejecutada);

        // Si la fecha no cambia, la propia subtarea ya está en la suma: se resta
        // para no contarla dos veces.
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

        fijarLineaBaseSiFalta(subtarea, dto.nuevaFecha());
        subtarea.setFechaObjetivo(dto.nuevaFecha());
        subtarea.setHorasEstimadas(dto.nuevasHoras());
        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public SubtareaDTO cambiarEstado(Integer usuarioId, Integer id, EstadoSubtareaDTO dto) {
        if (dto == null || dto.estado() == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }

        Subtarea subtarea = buscarEntidad(usuarioId, id);
        subtarea.setEstado(dto.estado());
        subtarea.setNotaExplicativa(dto.notaExplicativa());
        return aDto(subtareaRepository.save(subtarea));
    }

    @Transactional
    public void eliminarSubtarea(Integer usuarioId, Integer id) {
        Subtarea subtarea = buscarEntidad(usuarioId, id);
        Evento evento = subtarea.getEvento();
        evento.getSubtareas().remove(subtarea);
        subtareaRepository.delete(subtarea);
        subtareaRepository.flush();
    }

    /**
     * Busca el evento ya filtrado por propietario. Un evento ajeno responde con
     * el mismo mensaje que uno inexistente para no confirmar que el id existe
     * en la cuenta de otro usuario.
     */

    @Transactional(readOnly = true)
    public HoyResponseDTO obtenerHoyAgrupado(Integer usuarioId, LocalDate fecha) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El organizador es obligatorio");
        }
        LocalDate hoy = fecha == null ? LocalDate.now() : fecha;
        List<Subtarea> noEjecutadas = subtareaRepository.findNoEjecutadasHastaFecha(
                usuarioId,
                hoy.plusDays(30),
                EstadoSubtarea.ejecutada);

        List<SubtareaDTO> vencidas = new java.util.ArrayList<>();
        List<SubtareaDTO> paraHoy = new java.util.ArrayList<>();
        List<SubtareaDTO> proximas = new java.util.ArrayList<>();

        for (Subtarea subtarea : noEjecutadas) {
            SubtareaDTO dto = aDto(subtarea);
            LocalDate objetivo = subtarea.getFechaObjetivo();
            if (objetivo == null) {
                proximas.add(dto);
                continue;
            }
            if (objetivo.isBefore(hoy)) {
                vencidas.add(dto);
            } else if (objetivo.isEqual(hoy)) {
                paraHoy.add(dto);
            } else {
                proximas.add(dto);
            }
        }

        return new HoyResponseDTO(vencidas, paraHoy, proximas);
    }

    private Evento buscarEvento(Integer usuarioId, Integer eventoId) {
        return eventoRepository.findByIdYUsuarioId(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el evento con id " + eventoId));
    }

    private Subtarea buscarEntidad(Integer usuarioId, Integer id) {
        return subtareaRepository.findByIdYUsuarioId(id, usuarioId)
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

    /**
     * Guarda contra datos corruptos: la columna tiene CHECK en la base y
     * {@code UsuarioService} valida el rango al escribir, así que aquí solo se
     * evita que un valor imposible se interprete como un conflicto de capacidad.
     */
    private void validarLimiteDiario(int limiteDiario) {
        if (limiteDiario < UsuarioService.LIMITE_MINIMO_HORAS
                || limiteDiario > UsuarioService.LIMITE_MAXIMO_HORAS) {
            throw new IllegalStateException(
                    "El límite diario de " + limiteDiario + " horas está fuera del rango permitido");
        }
    }

    /**
     * Guarda la línea base de la gestión la primera vez que su fecha cambia.
     *
     * <p>Solo se fija una vez: reprogramar varias veces no va arrastrando la
     * base, así el desfase que ve el usuario siempre se mide contra la fecha con
     * la que se planificó. Si la fecha no cambia no se toca nada, para no marcar
     * como reprogramada una gestión que solo cambió de horas.
     */
    private void fijarLineaBaseSiFalta(Subtarea subtarea, LocalDate nuevaFecha) {
        if (subtarea.getFechaObjetivoOriginal() == null
                && !nuevaFecha.equals(subtarea.getFechaObjetivo())) {
            subtarea.setFechaObjetivoOriginal(subtarea.getFechaObjetivo());
        }
    }

    private SubtareaDTO aDto(Subtarea subtarea) {
        return new SubtareaDTO(
                subtarea.getIdSubtarea(),
                subtarea.getEvento().getIdEvento(),
                subtarea.getNombreGestion(),
                subtarea.getFechaObjetivo(),
                subtarea.getFechaObjetivoOriginal(),
                subtarea.getHorasEstimadas(),
                subtarea.getEstado(),
                subtarea.getNotaExplicativa(),
                subtarea.getFechaCreacion());
    }
}
