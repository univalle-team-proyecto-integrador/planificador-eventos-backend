package uv.isj.planificadoreventosbackend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import uv.isj.planificadoreventosbackend.model.Evento;

/**
 * Extiende {@link Repository} y no {@code JpaRepository} a propósito: así
 * {@code findAll()} y {@code findById(id)} no existen y no se pueden filtrar
 * eventos de otra cuenta por descuido. Todo acceso se hace por propietario.
 */
public interface EventoRepository extends Repository<Evento, Integer> {

    @EntityGraph(attributePaths = {"usuario", "tipoEvento"})
    @Query("SELECT e FROM Evento e WHERE e.usuario.idUsuario = :usuarioId")
    List<Evento> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @EntityGraph(attributePaths = {"usuario", "tipoEvento"})
    @Query("SELECT e FROM Evento e WHERE e.idEvento = :id AND e.usuario.idUsuario = :usuarioId")
    Optional<Evento> findByIdYUsuarioId(
            @Param("id") Integer id,
            @Param("usuarioId") Integer usuarioId);

    <S extends Evento> S save(S evento);

    void delete(Evento evento);

    void flush();
}
