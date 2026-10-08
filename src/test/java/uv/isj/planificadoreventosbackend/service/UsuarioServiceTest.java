package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    /**
     * Suma de horas para cualquier fecha. Todos los argumentos usan matcher:
     * Mockito no permite mezclar literales y matchers en la misma llamada.
     */
    private void stubSumaDeHoras(long total) {
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                eq(1), any(LocalDate.class), eq(EstadoSubtarea.ejecutada)))
                .thenReturn(total);
    }

    @Test
    void obtenerCapacidadProyectaLoComprometidoSobreElLimite() {
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

    /**
     * La exclusión de las ejecutadas la hace la consulta del repositorio; aquí
     * se verifica que el service pide exactamente el estado que la excluye.
     */
    @Test
    void obtenerCapacidadPideLaSumaExcluyendoLasEjecutadas() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(4L);

        usuarioService.obtenerCapacidad(1, FECHA);

        verify(subtareaRepository).sumarHorasNoEjecutadasPorFechaYUsuario(
                1, FECHA, EstadoSubtarea.ejecutada);
    }

    @Test
    void lasHorasDisponiblesNoBajanDeCeroEnUnaFechaSobrecargada() {
        usuario.setLimiteHorasDiarias(2);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(5L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, FECHA);

        assertThat(resultado.horasPlanificadas()).isEqualTo(5);
        assertThat(resultado.horasDisponibles()).isZero();
    }

    @Test
    void sinFechaSeEvaluaElHoyDeColombia() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        stubSumaDeHoras(0L);

        CapacidadDTO resultado = usuarioService.obtenerCapacidad(1, null);

        assertThat(resultado.fecha()).isEqualTo(LocalDate.now(ZoneId.of("America/Bogota")));
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

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void obtenerCapacidadDeUnUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerCapacidad(9999, FECHA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void actualizarLimiteDeUnUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarLimite(9999, 8))
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

        verify(usuarioRepository, never()).findById(anyInt());
    }
}