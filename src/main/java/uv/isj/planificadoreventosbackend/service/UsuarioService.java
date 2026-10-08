package uv.isj.planificadoreventosbackend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.CapacidadDTO;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    public static final int LIMITE_MINIMO_HORAS = 1;
    public static final int LIMITE_MAXIMO_HORAS = 16;

    private final UsuarioRepository usuarioRepository;
    private final SubtareaRepository subtareaRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            SubtareaRepository subtareaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.subtareaRepository = subtareaRepository;
    }

    @Transactional(readOnly = true)
    public CapacidadDTO obtenerCapacidad(Integer usuarioId, LocalDate fecha) {
        Usuario usuario = buscarEntidad(usuarioId);
        return calcularCapacidad(usuario, fecha == null ? hoy() : fecha);
    }

    @Transactional
    public CapacidadDTO actualizarLimite(Integer usuarioId, Integer limiteHorasDiarias) {
        validarLimite(limiteHorasDiarias);
        Usuario usuario = buscarEntidad(usuarioId);
        usuario.setLimiteHorasDiarias(limiteHorasDiarias);
        usuarioRepository.save(usuario);
        return calcularCapacidad(usuario, hoy());
    }

    private CapacidadDTO calcularCapacidad(Usuario usuario, LocalDate fecha) {
        int limite = usuario.getLimiteHorasDiarias();
        int horasPlanificadas = (int) subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                usuario.getIdUsuario(),
                fecha,
                EstadoSubtarea.ejecutada);
        return new CapacidadDTO(
                usuario.getIdUsuario(),
                limite,
                fecha,
                horasPlanificadas,
                Math.max(0, limite - horasPlanificadas));
    }

    private Usuario buscarEntidad(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El organizador es obligatorio");
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + usuarioId));
    }

    private void validarLimite(Integer limiteHorasDiarias) {
        if (limiteHorasDiarias == null) {
            throw new IllegalArgumentException("El límite de horas diarias es obligatorio");
        }
        if (limiteHorasDiarias < LIMITE_MINIMO_HORAS || limiteHorasDiarias > LIMITE_MAXIMO_HORAS) {
            throw new IllegalArgumentException(
                    "El límite de horas diarias debe estar entre "
                            + LIMITE_MINIMO_HORAS + " y " + LIMITE_MAXIMO_HORAS + " horas");
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }
}