package uv.isj.planificadoreventosbackend.exception;

import java.time.LocalDate;

/**
 * Se lanza cuando una reprogramación deja al organizador con más horas
 * comprometidas de las que permite su límite diario. La operación se revierte
 * y el manejador global la traduce a un 409 con el detalle del excedente.
 */
public class CapacidadExcedidaException extends RuntimeException {

    private final Integer idSubtarea;
    private final LocalDate fecha;
    private final int limiteDiario;
    private final int horasAsignadasPreviamente;
    private final int horasSolicitadas;
    private final int horasPlanificadasTotales;
    private final int excedente;

    public CapacidadExcedidaException(
            Integer idSubtarea,
            LocalDate fecha,
            int limiteDiario,
            int horasAsignadasPreviamente,
            int horasSolicitadas,
            int horasPlanificadasTotales) {
        super("La reprogramación supera el límite diario de " + limiteDiario + " horas");
        this.idSubtarea = idSubtarea;
        this.fecha = fecha;
        this.limiteDiario = limiteDiario;
        this.horasAsignadasPreviamente = horasAsignadasPreviamente;
        this.horasSolicitadas = horasSolicitadas;
        this.horasPlanificadasTotales = horasPlanificadasTotales;
        this.excedente = horasPlanificadasTotales - limiteDiario;
    }

    public Integer getIdSubtarea() {
        return idSubtarea;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getLimiteDiario() {
        return limiteDiario;
    }

    public int getHorasAsignadasPreviamente() {
        return horasAsignadasPreviamente;
    }

    public int getHorasSolicitadas() {
        return horasSolicitadas;
    }

    public int getHorasPlanificadasTotales() {
        return horasPlanificadasTotales;
    }

    public int getExcedente() {
        return excedente;
    }
}