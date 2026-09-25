package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoEventoRepository tipoEventoRepository;

    @Mock
    private SubtareaRepository subtareaRepository;

    private EventoService eventoService;

    private Usuario usuario;
    private TipoEvento tipoEvento;

    @BeforeEach
    void setUp() {
        eventoService = new EventoService(
                eventoRepository, usuarioRepository, tipoEventoRepository, subtareaRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Organizador");

        tipoEvento = new TipoEvento();
        tipoEvento.setIdTipoEvento(2);
        tipoEvento.setNombre("Boda");
    }

    private Evento eventoBase(Integer id) {
        Evento evento = new Evento();
        evento.setIdEvento(id);
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipoEvento);
        evento.setNombre("Boda de María y Luis");
        evento.setCliente("María");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Cali");
        evento.setFechaCreacion(LocalDateTime.of(2026, 9, 24, 10, 0));
        return evento;
    }

    private EventoDTO dtoValido(String nombre) {
        return new EventoDTO(
                null, 1, 2, nombre, "María", LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null);
    }

    @Test
    void listaEventosDelUsuarioYDevuelveLaListaVaciaDeSubtareas() {
        when(eventoRepository.findByUsuarioId(1)).thenReturn(List.of(eventoBase(5)));

        List<EventoDTO> resultado = eventoService.obtenerTodos(1);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Boda de María y Luis");
        assertThat(resultado.get(0).idUsuario()).isEqualTo(1);
        assertThat(resultado.get(0).subtareas()).isEmpty();
        verify(eventoRepository).findByUsuarioId(1);
        verify(eventoRepository, never()).findAll();
    }

    @Test
    void listaTodosLosEventosCuandoNoSeFiltraPorUsuario() {
        when(eventoRepository.findAll()).thenReturn(List.of(eventoBase(1), eventoBase(2)));

        List<EventoDTO> resultado = eventoService.obtenerTodos(null);

        assertThat(resultado).hasSize(2);
        verify(eventoRepository).findAll();
        verify(eventoRepository, never()).findByUsuarioId(any());
    }

    @Test
    void elDetalleIncluyeLasSubtareasDelEvento() {
        Evento evento = eventoBase(7);
        Subtarea subtarea = new Subtarea();
        subtarea.setIdSubtarea(3);
        subtarea.setEvento(evento);
        subtarea.setNombreGestion("Confirmar proveedor");
        subtarea.setHorasEstimadas(4);
        when(eventoRepository.findById(7)).thenReturn(Optional.of(evento));
        when(subtareaRepository.findByEventoId(7)).thenReturn(List.of(subtarea));

        EventoDTO resultado = eventoService.obtenerPorId(7);

        assertThat(resultado.idEvento()).isEqualTo(7);
        assertThat(resultado.subtareas()).hasSize(1);
        assertThat(resultado.subtareas().get(0).nombreGestion()).isEqualTo("Confirmar proveedor");
        assertThat(resultado.subtareas().get(0).idEvento()).isEqualTo(7);
    }

    @Test
    void elDetalleDeUnEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.obtenerPorId(9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void crearEventoRecortaLosCamposYPersiste() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(tipoEventoRepository.findById(2)).thenReturn(Optional.of(tipoEvento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoDTO resultado = eventoService.crearEvento(dtoValido("  Boda de prueba  "));

        assertThat(resultado.nombre()).isEqualTo("Boda de prueba");
        assertThat(resultado.cliente()).isEqualTo("María");
        assertThat(resultado.idUsuario()).isEqualTo(1);
        assertThat(resultado.idTipoEvento()).isEqualTo(2);
        assertThat(resultado.subtareas()).isEmpty();
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    void crearEventoConUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 9999, 2, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void crearEventoConTipoInexistenteLanzaExcepcion() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(tipoEventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 9999, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void crearEventoRechazaDatosIncompletos() {
        assertThatThrownBy(() -> eventoService.crearEvento(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, null, "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "   ", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, null, 2, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuario");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, null, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de evento");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "Boda", null,
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cliente");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "Boda", "  ",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cliente");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "Boda", "María", null, "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lugar");

        assertThatThrownBy(() -> eventoService.crearEvento(
                        new EventoDTO(null, 1, 2, "Boda", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lugar");

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoModificaTodosLosCamposEditable() {
        Evento evento = eventoBase(8);
        when(eventoRepository.findById(8)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(tipoEventoRepository.findById(2)).thenReturn(Optional.of(tipoEvento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoDTO resultado = eventoService.actualizarEvento(
                8, dtoValido("  Boda actualizada  "));

        assertThat(resultado.nombre()).isEqualTo("Boda actualizada");
        assertThat(evento.getNombre()).isEqualTo("Boda actualizada");
        assertThat(resultado.idEvento()).isEqualTo(8);
    }

    @Test
    void actualizarEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.actualizarEvento(9999, dtoValido("Boda")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void actualizarEventoRechazaDatosIncompletosSinTocarElGuardado() {
        assertThatThrownBy(() -> eventoService.actualizarEvento(
                        8, new EventoDTO(null, 1, 2, "", "María",
                                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void eliminarEventoRegexistenteBorraConFlush() {
        Evento evento = eventoBase(9);
        when(eventoRepository.findById(9)).thenReturn(Optional.of(evento));

        eventoService.eliminarEvento(9);

        verify(eventoRepository).delete(evento);
        verify(eventoRepository).flush();
    }

    @Test
    void eliminarEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.eliminarEvento(9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }
}