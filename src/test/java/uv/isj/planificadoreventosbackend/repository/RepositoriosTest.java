package uv.isj.planificadoreventosbackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoriosTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private SubtareaRepository subtareaRepository;

    @Test
    void guardaYConsultaPorClaveNaturalEnUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("organizador@example.com");
        usuario.setPasswordHash("hash-seguro");
        usuario.setNombre("Ana Gómez");
        usuarioRepository.saveAndFlush(usuario);

        Optional<Usuario> porEmail = usuarioRepository.findByEmail("organizador@example.com");
        assertThat(porEmail).isPresent();
        assertThat(porEmail.get().getIdUsuario()).isEqualTo(usuario.getIdUsuario());
        assertThat(usuarioRepository.existsByEmail("organizador@example.com")).isTrue();
        assertThat(usuarioRepository.existsByEmail("nadie@example.com")).isFalse();
    }

    @Test
    void guardaYConsultaTipoEventoPorNombre() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Conferencia");
        tipoEventoRepository.saveAndFlush(tipo);

        Optional<TipoEvento> porNombre = tipoEventoRepository.findByNombre("Conferencia");
        assertThat(porNombre).isPresent();
        assertThat(porNombre.get().getIdTipoEvento()).isEqualTo(tipo.getIdTipoEvento());
    }

    @Test
    void guardaYConsultaEventosPorUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("eventos@example.com");
        usuario.setPasswordHash("hash");
        usuario.setNombre("Luis");
        usuarioRepository.saveAndFlush(usuario);

        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Boda");
        tipoEventoRepository.saveAndFlush(tipo);

        Evento primero = evento(usuario, tipo, "Boda de María");
        Evento segundo = evento(usuario, tipo, "Boda de Luis");
        eventoRepository.saveAndFlush(primero);
        eventoRepository.saveAndFlush(segundo);

        List<Evento> porUsuario = eventoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        assertThat(porUsuario).hasSize(2);
        assertThat(porUsuario).extracting(Evento::getNombre)
                .containsExactlyInAnyOrder("Boda de María", "Boda de Luis");
    }

    @Test
    void guardaYConsultaSubtareasPorEventoYEstado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("subtareas@example.com");
        usuario.setPasswordHash("hash");
        usuario.setNombre("Marta");
        usuarioRepository.saveAndFlush(usuario);

        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Corporativo");
        tipoEventoRepository.saveAndFlush(tipo);

        Evento evento = evento(usuario, tipo, "Lanzamiento de producto");
        eventoRepository.saveAndFlush(evento);

        Subtarea pendiente = new Subtarea();
        pendiente.setEvento(evento);
        pendiente.setNombreGestion("Enviar invitaciones");
        pendiente.setFechaObjetivo(LocalDate.of(2026, 11, 5));
        pendiente.setHorasEstimadas(2);

        Subtarea ejecutada = new Subtarea();
        ejecutada.setEvento(evento);
        ejecutada.setNombreGestion("Reservar salón");
        ejecutada.setFechaObjetivo(LocalDate.of(2026, 11, 1));
        ejecutada.setHorasEstimadas(4);
        ejecutada.setEstado(EstadoSubtarea.ejecutada);
        subtareaRepository.saveAndFlush(pendiente);
        subtareaRepository.saveAndFlush(ejecutada);

        assertThat(subtareaRepository.findByEvento_IdEvento(evento.getIdEvento())).hasSize(2);
        assertThat(subtareaRepository.findByEstado(EstadoSubtarea.ejecutada))
                .extracting(Subtarea::getNombreGestion)
                .containsExactly("Reservar salón");
        assertThat(subtareaRepository.findByEstado(EstadoSubtarea.pendiente))
                .extracting(Subtarea::getNombreGestion)
                .containsExactly("Enviar invitaciones");
    }

    private Evento evento(Usuario usuario, TipoEvento tipo, String nombre) {
        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre(nombre);
        evento.setCliente("María");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Salón El Tesoro");
        return evento;
    }
}