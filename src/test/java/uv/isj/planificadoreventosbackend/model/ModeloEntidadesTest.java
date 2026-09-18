package uv.isj.planificadoreventosbackend.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModeloEntidadesTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mapeaLasCuatroTablasDelEsquema() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Boda");

        Usuario usuario = new Usuario();
        usuario.setEmail("organizador@example.com");
        usuario.setPasswordHash("hash-seguro");
        usuario.setNombre("Ana Gómez");

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre("Boda de María y Luis");
        evento.setCliente("María");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Salón El Tesoro");

        Subtarea subtarea = new Subtarea();
        subtarea.setEvento(evento);
        subtarea.setNombreGestion("Confirmar proveedor de flores");
        subtarea.setFechaObjetivo(LocalDate.of(2026, 11, 10));
        subtarea.setHorasEstimadas(3);
        subtarea.setNotaExplicativa("Primer contacto con la florería");

        entityManager.persist(tipo);
        entityManager.persist(usuario);
        entityManager.persist(evento);
        entityManager.persist(subtarea);
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tipo_evento", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evento", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subtarea", Integer.class)).isEqualTo(1);
    }

    @Test
    void aplicaLosValoresPorDefectoQueReflejanElDDL() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Otro");
        entityManager.persist(tipo);

        Usuario usuario = new Usuario();
        usuario.setEmail("default@example.com");
        usuario.setPasswordHash("hash");
        usuario.setNombre("Por defecto");
        entityManager.persist(usuario);

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre("Evento");
        evento.setCliente("Cliente");
        evento.setFechaEvento(LocalDateTime.now());
        evento.setLugar("Cali");
        entityManager.persist(evento);

        Subtarea subtarea = new Subtarea();
        subtarea.setEvento(evento);
        subtarea.setNombreGestion("Enviar invitaciones");
        subtarea.setFechaObjetivo(LocalDate.now());
        subtarea.setHorasEstimadas(2);
        entityManager.persist(subtarea);
        entityManager.flush();

        assertThat(usuario.getLimiteHorasDiarias()).isEqualTo(6);
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.pendiente);
        assertThat(evento.getFechaCreacion()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM subtarea WHERE id_subtarea = ?",
                String.class, subtarea.getIdSubtarea())).isEqualTo("pendiente");
    }
}