package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.CapacidadDTO;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SubtareaRepository subtareaRepository;

    private UsuarioService usuarioService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, subtareaRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setEmail("organizador@example.com");
        usuario.setNombre("Organizador");
        usuario.setLimiteHorasDiarias(6);
    }

    @Test
    void obtenerCapacidadSumaLoComprometidoYCalculaLoDisponible() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                1, FECHA, EstadoSubtarea.ejecutada)).thenReturn(5L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, FECHA);

        assertThat(resultado.usuarioId()).isEqualTo(1);
        assertThat(resultado.limiteHorasDiarias()).isEqualTo(6);
        assertThat(resultado.fecha()).isEqualTo(FECHA);
        assertThat(resultado.horasPlanificadas()).isEqualTo(5);
        assertThat(resultado.horasDisponibles()).isEqualTo(1);
    }

    @Test
    void obtenerCapacidadNoCuentaLasSubtareasEjecutadas() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                1, FECHA, EstadoSubtarea.ejecutada)).thenReturn(4L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, FECHA);

        // La consulta del repositorio ya excluye las ejecutadas; el service
        // solo proyecta ese total sobre el límite del organizador.
        assertThat(resultado.horasPlanificadas()).isEqualTo(4);
        assertThat(resultado.horasDisponibles()).isEqualTo(2);
    }

    @Test
    void lasHorasDisponiblesNoBajanDeCeroConUnaFechaSobrecargada() {
        usuario.setLimiteHorasDiarias(2);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                1, FECHA, EstadoSubtarea.ejecutada)).thenReturn(5L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, FECHA);

        assertThat(resultado.horasPlanificadas()).isEqualTo(5);
        assertThat(resultado.horasDisponibles()).isZero();
    }

    /**
     * Fija la suma de horas del organizador 1 para cualquier fecha. Todos los
     * argumentos usan matcher: Mockito no permite mezclar valores literales y
     * matchers en la misma llamada.
     */
    private void stubSumaDeHoras(long total) {
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                ArgumentMatchers.eq(1),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.eq(EstadoSubtarea.ejecutada)))
                .thenReturn(total);
    }

    @Test
    void obtenerCapacidadUriaFechaDeHoyDeColombiaSiSeOmite() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(0L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, null);

        LocalDate hoyBogota = LocalDate.now(ZoneId.of("America/Bogota"));
        assertThat(resultado.fecha()).isEqualTo(hoyBogota);
        assertThat(resultado.horasDisponibles()).isEqualTo(6);
    }

    @Test
    void actualizarLimiteGuardaYDevuelveLaCapacidadRecalculada() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(4L);

        CapacidadDTO resultado = usuarioService.actualizarLimite(1, 8);

        assertThat(usuario.getLimiteHorasDiarias()).isEqualTo(8);
        assertThat(resultado.limiteHorasDiarias()).isEqualTo(8);
        assertThat(resultado.horasDisponibles()).isEqualTo(4);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarLimiteAceptaLosExtremosDelRango() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(0L);

        assertThat(usuarioService.actualizarLimite(1, UsuarioService.LIMITE_MINIMO_HORAS)
                .limiteHorasDiarias()).isEqualTo(1);
        assertThat(usuarioService.actualizarLimite(1, UsuarioService.LIMITE_MAXIMO_HORAS)
                .limiteHorasDiarias()).isEqualTo(16);
    }

    @Test
    void actualizarLimiteRechazaValoresFueraDelRangoSinGuardar() {
        assertThatThrownBy(() -> usuarioService.actualizarLimite(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 16 horas");
        assertThatThrownBy(() -> usuarioService.actualizarLimite(1, 17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 16 horas");
        assertThatThrownBy(() -> usuarioService.actualizarLimite(1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");

        verify(usuarioRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void actualizarLimiteDeUnUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarLimite(9999, 8))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void obtenerCapacidadDeUnUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerCapacidad(9999, FECHA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void obtenerCapacidadRechazaUnOrganizadorInvalido() {
        assertThatThrownBy(() -> usuarioService.obtenerCapacidad(0, FECHA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> usuarioService.obtenerCapacidad(null, FECHA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");

        verify(usuarioRepository, never()).findById(ArgumentMatchers.any());
    }
}