package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiciosTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TipoEventoService tipoEventoService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private SubtareaService subtareaService;

    @Test
    void usuarioSaveInsertaConPasswordHashPlaceholder() {
        Usuario usuario = new Usuario();
        usuario.setEmail("servicio@example.com");
        usuario.setNombre("Ana");

        Usuario guardado = usuarioService.save(usuario);

        assertThat(guardado.getIdUsuario()).isNotNull();
        assertThat(guardado.getPasswordHash()).isEqualTo("cambiar-contrasena");
        assertThat(usuarioService.findById(guardado.getIdUsuario())).isPresent();
        assertThat(usuarioService.findByEmail("servicio@example.com")).isPresent();
        assertThat(usuarioService.existsByEmail("servicio@example.com")).isTrue();
    }

    @Test
    void saveConIdExistenteActualizaElRegistro() {
        Usuario usuario = new Usuario();
        usuario.setEmail("actualizar@example.com");
        usuario.setNombre("Luis");
        Usuario guardado = usuarioService.save(usuario);

        guardado.setNombre("Luis Fernando");
        usuarioService.save(guardado);

        Usuario recuperado = usuarioService.findById(guardado.getIdUsuario()).orElseThrow();
        assertThat(recuperado.getNombre()).isEqualTo("Luis Fernando");
    }

    @Test
    void tipoEventoSaveYConsultaPorNombre() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Conferencia");

        TipoEvento guardado = tipoEventoService.save(tipo);

        assertThat(guardado.getIdTipoEvento()).isNotNull();
        assertThat(tipoEventoService.findByNombre("Conferencia")).isPresent();
    }

    @Test
    void eventoYSubtareaConFksAsignadosPorElLlamador() {
        Usuario usuario = usuario("eventos@example.com");
        TipoEvento tipo = tipoEvento("Corporativo");
        usuarioService.save(usuario);
        tipoEventoService.save(tipo);

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre("Lanzamiento de producto");
        evento.setCliente("Empresa X");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 9, 0));
        evento.setLugar("Centro de eventos");
        Evento eventoGuardado = eventoService.save(evento);

        Subtarea subtarea = new Subtarea();
        subtarea.setEvento(eventoGuardado);
        subtarea.setNombreGestion("Preparar sala");
        subtarea.setFechaObjetivo(LocalDate.of(2026, 11, 20));
        subtarea.setHorasEstimadas(2);
        Subtarea subtareaGuardada = subtareaService.save(subtarea);

        assertThat(eventoService.findByUsuario(usuario.getIdUsuario())).hasSize(1);
        assertThat(eventoService.findById(eventoGuardado.getIdEvento())).isPresent();
        assertThat(subtareaService.findByEvento(eventoGuardado.getIdEvento())).hasSize(1);
        assertThat(subtareaGuardada.getEstado()).isEqualTo(EstadoSubtarea.pendiente);
    }

    @Test
    void deleteEliminaYDeleteDeInexistenteLanzaExcepcion() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Boda");
        TipoEvento guardado = tipoEventoService.save(tipo);

        tipoEventoService.delete(guardado.getIdTipoEvento());

        assertThat(tipoEventoService.findById(guardado.getIdTipoEvento())).isEmpty();
        assertThatThrownBy(() -> tipoEventoService.delete(999999))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre("Marta");
        return usuario;
    }

    private TipoEvento tipoEvento(String nombre) {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre(nombre);
        return tipo;
    }
}