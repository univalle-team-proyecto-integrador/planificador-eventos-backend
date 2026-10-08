package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uv.isj.planificadoreventosbackend.exception.CapacidadExcedidaException;
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
        when(eventoRepository.findByIdYUsuarioId(10, 1)).thenReturn(Optional.of(evento));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));
        SubtareaDTO dto = new SubtareaDTO(
                null, null, "  Confirmar catering  ", LocalDate.of(2026, 11, 15), 4,
                EstadoSubtarea.ejecutada, "Nota previa", null);

        SubtareaDTO resultado = subtareaService.agregarSubtarea(1, 10, dto);

        assertThat(resultado.nombreGestion()).isEqualTo("Confirmar catering");
        assertThat(resultado.estado()).isEqualTo(EstadoSubtarea.pendiente);
        assertThat(resultado.horasEstimadas()).isEqualTo(4);
        assertThat(resultado.idEvento()).isEqualTo(10);
        assertThat(resultado.notaExplicativa()).isEqualTo("Nota previa");
    }

    @Test
    void agregarSubtareaRechazaDatosIncompletos() {
        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, null, dtoAgregar("T", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evento");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, dtoAgregar("  ", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, dtoAgregar("T", null, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, dtoAgregar("T", LocalDate.of(2026, 11, 10), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, dtoAgregar("T", LocalDate.of(2026, 11, 10), 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 10, dtoAgregar("T", LocalDate.of(2026, 11, 10), -3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void agregarSubtareaAEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findByIdYUsuarioId(9999, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.agregarSubtarea(1, 
                        9999, dtoAgregar("T", LocalDate.of(2026, 11, 10), 2)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void actualizarSubtareaModificaLosCamposEditables() {
        Subtarea subtarea = subtareaCreada(3, "Gestión anterior", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(3, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.actualizarSubtarea(1, 
                3, new SubtareaActualizacionDTO("  Gestión nueva  ", LocalDate.of(2026, 11, 15), 4));

        assertThat(resultado.nombreGestion()).isEqualTo("Gestión nueva");
        assertThat(resultado.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(resultado.horasEstimadas()).isEqualTo(4);
    }

    @Test
    void actualizarSubtareaRechazaDatosIncompletos() {
        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(1, 3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(1, 
                        3, new SubtareaActualizacionDTO(" ", LocalDate.of(2026, 11, 15), 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(1, 
                        3, new SubtareaActualizacionDTO("T", null, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(1, 
                        3, new SubtareaActualizacionDTO("T", LocalDate.of(2026, 11, 15), 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void actualizarSubtareaInexistenteLanzaExcepcion() {
        when(subtareaRepository.findByIdYUsuarioId(9999, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.actualizarSubtarea(1, 
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
        when(eventoRepository.findByIdYUsuarioId(10, 1)).thenReturn(Optional.of(evento));
        when(subtareaRepository.findByEventoIdYUsuarioId(10, 1)).thenReturn(List.of(primera, segunda));

        List<SubtareaDTO> resultado = subtareaService.obtenerPorEvento(1, 10);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombreGestion()).isEqualTo("Primera");
        assertThat(resultado.get(1).estado()).isEqualTo(EstadoSubtarea.ejecutada);
    }

    @Test
    void listarSubtareasDeEventoInexistenteLanzaExcepcion() {
        when(eventoRepository.findByIdYUsuarioId(9999, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.obtenerPorEvento(1, 9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void obtenerPorIdDevuelveElMapeoCompletoDeLaSubtarea() {
        Subtarea subtarea = subtareaCreada(9, "Tarea de detalle", LocalDate.of(2026, 11, 18), 2,
                EstadoSubtarea.ejecutada);
        subtarea.setNotaExplicativa("Detalle entregado");
        when(subtareaRepository.findByIdYUsuarioId(9, 1)).thenReturn(Optional.of(subtarea));

        SubtareaDTO resultado = subtareaService.obtenerPorId(1, 9);

        assertThat(resultado.idSubtarea()).isEqualTo(9);
        assertThat(resultado.idEvento()).isEqualTo(10);
        assertThat(resultado.nombreGestion()).isEqualTo("Tarea de detalle");
        assertThat(resultado.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(resultado.horasEstimadas()).isEqualTo(2);
        assertThat(resultado.estado()).isEqualTo(EstadoSubtarea.ejecutada);
        assertThat(resultado.notaExplicativa()).isEqualTo("Detalle entregado");
    }

    @Test
    void obtenerPorIdDeSubtareaInexistenteLanzaExcepcion() {
        when(subtareaRepository.findByIdYUsuarioId(9999, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.obtenerPorId(1, 9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void listarGestionesParaHoyUsaLaFechaDeBogotaCuandoNoSeEnvia() {
        LocalDate esperada = LocalDate.now(ZoneId.of("America/Bogota"));
        when(subtareaRepository.findNoEjecutadasParaHoy(
                eq(1), any(LocalDate.class), eq(EstadoSubtarea.ejecutada)))
                .thenReturn(List.of());

        subtareaService.obtenerParaHoy(1, null);

        ArgumentCaptor<LocalDate> fecha = ArgumentCaptor.forClass(LocalDate.class);
        verify(subtareaRepository).findNoEjecutadasParaHoy(
                eq(1), fecha.capture(), eq(EstadoSubtarea.ejecutada));
        assertThat(fecha.getValue())
                .isBetween(esperada.minusDays(1), esperada.plusDays(1));
    }

    @Test
    void listarGestionesParaHoyRespetaLaFechaEnviada() {
        LocalDate solicitada = LocalDate.of(2026, 9, 25);
        when(subtareaRepository.findNoEjecutadasParaHoy(
                1, solicitada, EstadoSubtarea.ejecutada))
                .thenReturn(List.of());

        subtareaService.obtenerParaHoy(1, solicitada);

        verify(subtareaRepository)
                .findNoEjecutadasParaHoy(1, solicitada, EstadoSubtarea.ejecutada);
    }

    @Test
    void listarGestionesParaHoyRechazaOrganizadoresInvalidos() {
        assertThatThrownBy(() -> subtareaService.obtenerParaHoy(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organizador");

        assertThatThrownBy(() -> subtareaService.obtenerParaHoy(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organizador");

        verify(subtareaRepository, never())
                .findNoEjecutadasParaHoy(anyInt(), any(LocalDate.class), any(EstadoSubtarea.class));
    }

    /**
 * El límite ya no se pide por separado: reprogramar lo toma del usuario del
 * evento, así que la prueba verifica que un límite ajeno no entre en el cálculo.
 */
    @Test
    void reprogramarTomaElLimiteDelUsuarioDelEvento() {
        usuario.setLimiteHorasDiarias(4);
        Subtarea subtarea = subtareaCreada(1, "T", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                1, LocalDate.of(2026, 11, 15), EstadoSubtarea.ejecutada)).thenReturn(4L);

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 15), 1)))
                .isInstanceOf(CapacidadExcedidaException.class)
                .satisfies(excepcion -> assertThat(
                                ((CapacidadExcedidaException) excepcion).getLimiteDiario())
                        .isEqualTo(4));
    }

    @Test
    void cambiarEstadoActualizaEstadoYNota() {
        Subtarea subtarea = subtareaCreada(4, "Tarea para completar", LocalDate.of(2026, 11, 12), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(4, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.cambiarEstado(1, 
                4, new EstadoSubtareaDTO(EstadoSubtarea.pospuesta, "Proveedor sin disponibilidad"));

        assertThat(resultado.estado()).isEqualTo(EstadoSubtarea.pospuesta);
        assertThat(resultado.notaExplicativa()).isEqualTo("Proveedor sin disponibilidad");
    }

    @Test
    void cambiarEstadoRechazaDtoNuloOEstadoNulo() {
        assertThatThrownBy(() -> subtareaService.cambiarEstado(1, 4, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado");

        assertThatThrownBy(() -> subtareaService.cambiarEstado(1, 4, new EstadoSubtareaDTO(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void eliminarSubtareaLaQuitaDelEventoYBorra() {
        Subtarea subtarea = subtareaCreada(7, "Tarea a borrar", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(7, 1)).thenReturn(Optional.of(subtarea));

        subtareaService.eliminarSubtarea(1, 7);

        assertThat(evento.getSubtareas()).doesNotContain(subtarea);
        verify(subtareaRepository).delete(subtarea);
        verify(subtareaRepository).flush();
    }

    @Test
    void reprogramarDetectaConflictoSinModificarLaSubtarea() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(6L);

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 3)))
                .isInstanceOf(CapacidadExcedidaException.class)
                .satisfies(excepcion -> {
                    CapacidadExcedidaException conflicto = (CapacidadExcedidaException) excepcion;
                    assertThat(conflicto.getLimiteDiario()).isEqualTo(5);
                    assertThat(conflicto.getHorasAsignadasPreviamente()).isEqualTo(6);
                    assertThat(conflicto.getHorasSolicitadas()).isEqualTo(3);
                    assertThat(conflicto.getHorasPlanificadasTotales()).isEqualTo(9);
                    assertThat(conflicto.getExcedente()).isEqualTo(4);
                    assertThat(conflicto.getFecha()).isEqualTo(LocalDate.of(2026, 11, 20));
                    assertThat(conflicto.getIdSubtarea()).isEqualTo(1);
                });

        assertThat(subtarea.getFechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(subtarea.getHorasEstimadas()).isEqualTo(1);
        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void reprogramarAplicaElCambioCuandoNoSuperaElLimite() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 21), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.reprogramar(
                1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 21), 3));

        assertThat(subtarea.getFechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(subtarea.getHorasEstimadas()).isEqualTo(3);
        assertThat(resultado.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(resultado.horasEstimadas()).isEqualTo(3);
    }

    @Test
    void reprogramarEnLaMismaFechaNoCuentaDosVecesLaMismaSubtarea() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 20), 2,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(5L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.reprogramar(
                1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 2));

        assertThat(resultado.horasEstimadas()).isEqualTo(2);
    }

    @Test
    void reprogramarASumaMenorQueLaPropiaSatisfaceElLimite() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 20), 3,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.reprogramar(
                1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 3));

        assertThat(resultado.horasEstimadas()).isEqualTo(3);
    }

    @Test
    void reprogramarUnaEjecutadaEnLaMismaFechaNoRestaSusHoras() {
        Subtarea subtarea = subtareaCreada(1, "Tarea ejecutada", LocalDate.of(2026, 11, 20), 2,
                EstadoSubtarea.ejecutada);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 20), EstadoSubtarea.ejecutada)).thenReturn(5L);

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 20), 2)))
                .isInstanceOf(CapacidadExcedidaException.class)
                .satisfies(excepcion -> assertThat(
                                ((CapacidadExcedidaException) excepcion).getExcedente())
                        .isEqualTo(2));

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void reprogramarJustoAlLimiteNoReportaConflicto() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 21), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(1,
                LocalDate.of(2026, 11, 22), EstadoSubtarea.ejecutada)).thenReturn(2L);
        when(subtareaRepository.save(any(Subtarea.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtareaDTO resultado = subtareaService.reprogramar(
                1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 3));

        assertThat(resultado.fechaObjetivo()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(resultado.horasEstimadas()).isEqualTo(3);
    }

    @Test
    void reprogramarRechazaDatosInvalidos() {
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 21), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));

        assertThatThrownBy(() -> subtareaService.reprogramar(1, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(null, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horas");

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horas");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    /**
     * Guarda contra datos corruptos: con el CHECK de la base y la validación del
     * PUT un límite inválido no debería llegar aquí, pero si llega no debe
     * disfrazarse de un conflicto de capacidad.
     */
    @Test
    void reprogramarConLimiteFueraDeRangoFallaSinGuardar() {
        usuario.setLimiteHorasDiarias(0);
        Subtarea subtarea = subtareaCreada(1, "Tarea", LocalDate.of(2026, 11, 21), 1,
                EstadoSubtarea.pendiente);
        when(subtareaRepository.findByIdYUsuarioId(1, 1)).thenReturn(Optional.of(subtarea));

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 1, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fuera del rango permitido");

        verify(subtareaRepository, never()).save(any(Subtarea.class));
    }

    @Test
    void reprogramarSubtareaInexistenteLanzaExcepcion() {
        when(subtareaRepository.findByIdYUsuarioId(9999, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtareaService.reprogramar(
                        1, 9999, new ReprogramarDTO(LocalDate.of(2026, 11, 22), 2)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }
}