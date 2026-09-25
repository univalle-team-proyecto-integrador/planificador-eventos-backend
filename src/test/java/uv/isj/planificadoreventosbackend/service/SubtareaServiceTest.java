package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EstadoSubtareaDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaActualizacionDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;

@ExtendWith(MockitoExtension.class)
class SubtareaServiceTest {

    @Mock
    private SubtareaRepository subtareaRepository;

    @Mock
    private EventoRepository eventoRepository;

    private SubtareaService subtareaService;

    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        subtareaService = new SubtareaService(subtareaRepository, eventoRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setLimiteHorasDiarias(5);

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setIdTipoEvento(2);

        evento = new Evento();
        evento.setIdEvento(10);
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipoEvento);
    }

    private Subtarea subtareaCreada(
            int id, String nombre, LocalDate fecha, int horas, EstadoSubtarea estado) {
        Subtarea subtarea = new Subtarea();
        subtarea.setIdSubtarea(id);
        evento.agregarSubtarea(subtarea);
        subtarea.setNombreGestion(nombre);
        subtarea.setFechaObjetivo(fecha);
        subtarea.setHorasEstimadas(horas);
        subtarea.setEstado(estado);
        return subtarea;
    }

    private SubtareaDTO dtoAgregar(String nombre, LocalDate fecha, Integer horas) {
        return new SubtareaDTO(null, null, nombre, fecha, horas, null, null, null);
    }

    @Test
    void agregarSubtareaFuerzaEstadoPendienteYRecortaElNombre() {
        when(eventoRepository.findById(10)).thenReturn(Optional.of(evento));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));
        SubtareaDTO dto = new SubtareaDTO(
                null, null, "  Confirmar catering  ", LocalDate.of(2026, 11, 15), 4,
                EstadoSubtarea.ejecutada, "Nota previa", null);

        SubtareaDTO resultado = subtareaService.agregarSubtarea(10, dto);

        assertThat(resultado.nombreGestion()).isEqualTo("Confirmar catering");
        assertThat(resultado.estado()).isEqualTo(EstadoSubtarea.pendiente);
        assertThat(resultado.horasEstimadas()).isEqualTo(4);
        assertThat(resultado.idEvento()).isEqualTo(10);
        assertThat(resultado.notaExplicativa()).isEqualTo("Nota previa");
    }

    @Test
    void agregarSubtareaRechazaDatosIncompletos() {
        assertThatThrownBy(() -> subtareaService.agregarSubtarea(null, dtoAgregar("T", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evento");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, dtoAgregar("  ", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, dtoAgregar("T", null, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, dtoAgregar("T", LocalDate.of(2026, 11, 10), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, dtoAgregar("T", LocalDate.of(2026, 11, 10), 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(10, dtoAgregar("T", LocalDate.of(2026, 11, 10), -3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void agregarSubtareaAEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(
                        9999, dtoAgregar("T", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void actualizarSubtareaModificaLosCamposEditables() {
        Subtarea subtarea = subtareaCreada(3, "Gestión anterior", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(3)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.actualizarSubtarea(
                3, new SubtareaActualizacionDTO("  Gestión nueva  ", LocalDate.of(2026, 11, 15), 4));

        assertThat(resultado.nombreGestion()).isEqualTo("Gestión nueva");
        assertThat(resultado.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(resultado.horasEstimadas()).isEqualTo(4);
    }

    @Test
    void actualizarSubtareaRechazaDatosIncompletos() {
        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(
                        3, new SubtareaActualizacionDTO(" ", LocalDate.of(2026, 11, 15), 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(
                        3, new SubtareaActualizacionDTO("T", null, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(
                        3, new SubtareaActualizacionDTO("T", LocalDate.of(2026, 11, 15), 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void actualizarSubtareaInexistenteLanzaExcepcion() {
        when(subtareaRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(
                        9999, new SubtareaActualizacionDTO("T", LocalDate.of(2026, 11, 15), 2)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void listarSubtareasDeUnEventoDevuelveElMapeoCompleto() {
        Subtarea primera = subtareaCreada(1, "Primera", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        Subtarea segunda = subtareaCreada(2, "Segunda", LocalDate.of(2026, 11, 11), 3,
                EstadoSubtarea.ejecutada);
        when(eventoRepository.findById(10)).thenReturn(Optional.of(evento));
        when(subtareaRepository.findByEventoId(10)).thenReturn(List.of(primera, segunda));

        List<SubtareaDTO> resultado = subtareaService.obtenerPorEvento(10);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombreGestion()).isEqualTo("Primera");
        assertThat(resultado.get(1).estado()).isEqualTo(EstadoSubtarea.ejecutada);
    }

    @Test
    void listarSubtareasDeEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.obtenerPorEvento(9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void obtenerLimiteDiarioVieneDelUsuarioDelEvento() {
        Subtarea subtarea = subtareaCreada(1, "T", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));

        assertThat(subtareaService.obtenerLimiteDiario(1)).isEqualTo(5);
    }

    @Test
    void cambiarEstadoActualizaEstadoYNota() {
        Subtarea subtarea = subtareaCreada(4, "Tarea para completar", LocalDate.of(2026, 11, 12), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(4)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.cambiarEstado(
                4, new EstadoSubtareaDTO(EstadoSubtarea.pospuesta, "Proveedor sin disponibilidad"));

        assertThat(resultado.estado()).isEqualTo(EstadoSubtarea.pospuesta);
        assertThat(resultado.notaExplicativa()).isEqualTo("Proveedor sin disponibilidad");
    }

    @Test
    void cambiarEstadoRechazaDtoNuloOEstadoNulo() {
        assertThatThrownBy(() -> subtareaService.cambiarEstado(4, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado");

        assertThatThrownBy(() -> subtareaService.cambiarEstado(4, new EstadoSubtareaDTO(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void eliminarSubtareaLaQuitaDelEventoYBorra() {
        Subtarea subtarea = subtareaCreada(7, "Tarea a borrar", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(7)).thenReturn(Optional.of(subtarea));

        subtareaService.eliminarSubtarea(7);

        assertThat(evento.getSubtareas()).doesNotContain(subtarea);
        verify(subtareaRepository).delete(subtarea);
        verify(subtareaRepository).flush();
    }

    @Test
    void reprogramarDetectaConflictoSinModificarLaSubtarea() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(6L);

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 3), 5.0);

        assertThat(resultado.get("conflicto")).isEqualTo(true);
        assertThat(resultado.get("limiteDiario")).isEqualTo(5.0);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(9.0);
        assertThat(subtarea.getFechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(subtarea.getHorasEstimadas()).isEqualTo(1);
        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void reprogramarAplicaElCambioCuandoNoSuperaElLimite() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 21), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 21), 3), 5.0);

        SubtareaDTO subtareaRespuesta = (SubtareaDTO) resultado.get("subtarea");
        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(5.0);
        assertThat(subtarea.getFechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(subtarea.getHorasEstimadas()).isEqualTo(3);
        assertThat(subtareaRespuesta.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(subtareaRespuesta.horasEstimadas()).isEqualTo(3);
    }

    @Test
    void reprogramarEnLaMismaFechaNoCuentaDosVecesLaMismaSubtarea() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 20), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(5L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 2), 5.0);

        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(5.0);
    }

    @Test
    void reprogramarASumaMenorQueLaPropiaSatisfaceElLimite() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 20), 3,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 3), 5.0);

        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(3.0);
    }

    @Test
    void reprogramarUnaEjecutadaEnLaMismaFechaNoRestaSusHoras() {
        Subtarea subtarea = subtareaCreada(1, "Tarea ejecutada", LocalDate.of(2026, 11, 20), 2,
                EstadoSubtarea.ejecutada);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(5L);

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 2), 5.0);

        assertThat(resultado.get("conflicto")).isEqualTo(true);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(7.0);
    }

    @Test
    void reprogramarJustoAlLimiteNoReportaConflicto() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 21), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 22), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = subtareaService.reprogramar(
                1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 3), 5.0);

        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(5.0);
    }

    @Test
    void reprogramarRechazaDatosInvalidos() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 21), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findById(1)).thenReturn(Optional.of(subtarea));

        assertThatThrownBy(() -> subtareaService.reprogramar(1, null, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(null, 2), 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), null), 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horas");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 0), 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horas");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite diario");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2), 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite diario");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite diario");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void reprogramarSubtareaInexistenteLanzaExcepcion() {
        when(subtareaRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        9999, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2), 5.0))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }
}