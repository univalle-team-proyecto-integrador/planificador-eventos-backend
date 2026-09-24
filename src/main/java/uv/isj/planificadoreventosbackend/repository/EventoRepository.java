package uv.isj.planificadoreventosbackend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uv.isj.planificadoreventosbackend.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "tipoEvento"})
    List<Evento> findAll();

    @Override
    @EntityGraph(attributePaths = {"usuario", "tipoEvento"})
    Optional<Evento> findById(Integer id);

    @EntityGraph(attributePaths = {"usuario", "tipoEvento"})
    @Query("SELECT e FROM Evento e WHERE e.usuario.idUsuario = :usuarioId")
    List<Evento> findByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
