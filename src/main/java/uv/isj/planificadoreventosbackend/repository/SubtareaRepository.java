package uv.isj.planificadoreventosbackend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;

/**
 * Acceso a subtareas siempre acotado a un propietario.
 *
 * <p>Esta interfaz extiende {@link Repository}, el marcador vacío, y
 * <strong>no</strong> {@code JpaRepository} ni {@code CrudRepository}. Ambas
 * heredan {@code findAll()}, {@code findAllById()} y {@code findById(id)}, y los
 * métodos heredados no se pueden quitar de una interfaz: Spring Data los
 * implementa en {@code SimpleJpaRepository}. Con la interfaz marcadora, esos
 * métodos sencillamente no existen, y {@code save}/{@code flush} se declaran a
 * mano y se resuelven contra esa misma implementación base.
 *
 * <p>El efecto es que no hay forma de leer o modificar una subtarea sin dueño:
 * toda lectura exige el {@code usuarioId} propietario del evento, y
 * {@link #findByIdYUsuarioId} sustituye al {@code findById(id)} que permitía
 * editar o borrar la subtarea de otra cuenta.
 *
 * <p>SubtareaRepositoryAislamientoTest verifica por reflexión que estos métodos
 * no reaparezcan.
 */
public interface SubtareaRepository extends Repository<Subtarea, Integer> {

    <S extends Subtarea> S save(S entidad);

    void delete(Subtarea entidad);

    void flush();

    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.evento.idEvento = :eventoId
              AND s.evento.usuario.idUsuario = :usuarioId
            ORDER BY s.fechaObjetivo ASC, s.idSubtarea ASC
            """)
    List<Subtarea> findByEventoIdYUsuarioId(
            @Param("eventoId") Integer eventoId,
            @Param("usuarioId") Integer usuarioId);

    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.idSubtarea = :idSubtarea
              AND s.evento.usuario.idUsuario = :usuarioId
            """)
    Optional<Subtarea> findByIdYUsuarioId(
            @Param("idSubtarea") Integer idSubtarea,
            @Param("usuarioId") Integer usuarioId);

    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.fechaObjetivo = :fecha
              AND s.estado <> :estado
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC
            """)
    List<Subtarea> findNoEjecutadasParaHoy(
            @Param("usuarioId") Integer usuarioId,
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoSubtarea estado);

    @Query("""
            SELECT COALESCE(SUM(s.horasEstimadas), 0)
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.fechaObjetivo = :fecha
              AND s.estado <> :estado
            """)
    long sumarHorasNoEjecutadasPorFechaYUsuario(
            @Param("usuarioId") Integer usuarioId,
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoSubtarea estado);
}
