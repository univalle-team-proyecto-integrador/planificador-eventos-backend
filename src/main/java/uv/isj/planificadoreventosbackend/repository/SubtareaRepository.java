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
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC, s.idSubtarea ASC
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

    /**
     * Gestiones que planifican el día indicado.
     *
     * <p>Excluye `pospuesta` además de `ejecutada` por el mismo motivo que la
     * suma de horas: sin fecha efectiva la gestión no pertenece a ningún día.
     */
    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.fechaObjetivo = :fecha
              AND s.estado NOT IN :estadosSinCompromiso
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC
            """)
    List<Subtarea> findNoEjecutadasParaHoy(
            @Param("usuarioId") Integer usuarioId,
            @Param("fecha") LocalDate fecha,
            @Param("estadosSinCompromiso") List<EstadoSubtarea> estadosSinCompromiso);

    /**
     * Horas que comprometen un día concreto.
     *
     * <p>El estado va como colección y no como un enum suelto porque hay DOS
     * estados que no comprometen el día, no uno: `ejecutada` porque ya se hizo, y
     * `pospuesta` porque no tiene fecha efectiva conocida. Una gestión pospuesta
     * conserva la última fecha que tuvo, pero esa ya no es un compromiso: hasta
     * reprogramarla no se sabe cuándo se hará, así que sumarla sería contabilizar
     * sobre una fecha muerta. Se le pasa la lista al llamador para que la regla
     * viva en el servicio y no repetida en cada consulta.
     */
    @Query("""
            SELECT COALESCE(SUM(s.horasEstimadas), 0)
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.fechaObjetivo = :fecha
              AND s.estado NOT IN :estadosSinCompromiso
            """)
    long sumarHorasNoEjecutadasPorFechaYUsuario(
            @Param("usuarioId") Integer usuarioId,
            @Param("fecha") LocalDate fecha,
            @Param("estadosSinCompromiso") List<EstadoSubtarea> estadosSinCompromiso);

    /**
     * Gestiones que planifican los próximos días, agrupables en vencidas, para
     * hoy y próximas. Excluye `pospuesta` por la misma razón que las demás: sin
     * fecha efectiva no se ubica en ningún día.
     */
    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.estado NOT IN :estadosSinCompromiso
              AND s.fechaObjetivo <= :hasta
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC, s.idSubtarea ASC
            """)
    List<Subtarea> findNoEjecutadasHastaFecha(
            @Param("usuarioId") Integer usuarioId,
            @Param("hasta") LocalDate hasta,
            @Param("estadosSinCompromiso") List<EstadoSubtarea> estadosSinCompromiso);

    /**
     * Gestiones pospuestas del organizador, sin restricción de fecha.
     *
     * <p>Existe para que la posposición no sea un callejón sin salida: la gestión
     * no ocupa ningún día, pero tiene que poder listarse y reprogramarse.
     */
    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.evento.usuario.idUsuario = :usuarioId
              AND s.estado = :estado
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC, s.idSubtarea ASC
            """)
    List<Subtarea> findPospuestas(
            @Param("usuarioId") Integer usuarioId,
            @Param("estado") EstadoSubtarea estado);
}
