package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class Existencia {

    /** Escala de NUMERIC(12, 4) en 'existencias'. */
    private static final int ESCALA = 4;

    private ExistenciaId id;
    private BigDecimal contenidoInicial;
    private BigDecimal contenidoActual;
    private LocalDateTime fechaAcabado;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaApertura;
    private DetalleLoteId detalleLoteId;
    private EstadoExistenciaId estadoExistenciaId;
    private UnidadId unidadId;
    private UsuarioId usuarioId;
    private boolean borrado;

    private Existencia(
        ExistenciaId id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        this.id = id;
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaApertura = fechaApertura;
        this.detalleLoteId = detalleLoteId;
        this.estadoExistenciaId = estadoExistenciaId;
        this.unidadId = unidadId;
        this.usuarioId = usuarioId;
        this.borrado = borrado;
    }

    public static Existencia restaurar(
        String id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        return new Existencia(new ExistenciaId(id), contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, fechaApertura, detalleLoteId, estadoExistenciaId, unidadId, usuarioId, borrado);
    }

    public static Existencia crear(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        UsuarioId usuarioId
    ){
        return new Existencia(null, contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, fechaApertura, detalleLoteId, estadoExistenciaId, unidadId, usuarioId, false);
    }

    public void actualizar(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        UsuarioId usuarioId
    ){
        this.contenidoInicial = contenidoInicial != null ? contenidoInicial : this.contenidoInicial;
        this.contenidoActual = contenidoActual != null ? contenidoActual : this.contenidoActual;
        this.fechaAcabado = fechaAcabado != null ? fechaAcabado : this.fechaAcabado;
        this.fechaVencimiento = fechaVencimiento != null ? fechaVencimiento : this.fechaVencimiento;
        this.fechaApertura = fechaApertura != null ? fechaApertura : this.fechaApertura;
        this.detalleLoteId = detalleLoteId != null ? detalleLoteId : this.detalleLoteId;
        this.estadoExistenciaId = estadoExistenciaId != null ? estadoExistenciaId : this.estadoExistenciaId;
        this.unidadId = unidadId != null ? unidadId : this.unidadId;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
    }

    /**
     * Descuenta contenido por el consumo de una receta o por la merma de un pesaje (logica.md
     * MOVIMIENTO-18, EXISTENCIA-16). Nunca deja el contenido en negativo; al agotarse registra la
     * fecha en que se acabo y pasa al estado de agotada que indica quien consume, que registra
     * ademas el movimiento que explica el consumo.
     */
    public void consumir(BigDecimal cantidad, LocalDateTime fecha, EstadoExistenciaId estadoAgotada) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad a consumir de la existencia debe ser mayor a cero");
        }

        BigDecimal consumo = cantidad.setScale(ESCALA, RoundingMode.HALF_UP);
        if (consumo.compareTo(this.contenidoActual) > 0) {
            throw DomainException.throwException(409,
                    "La existencia con id '" + this.id.getValue() + "' no tiene contenido suficiente");
        }

        this.contenidoActual = this.contenidoActual.subtract(consumo).setScale(ESCALA, RoundingMode.HALF_UP);
        if (this.contenidoActual.signum() == 0) {
            this.fechaAcabado = fecha;
            this.estadoExistenciaId = estadoAgotada != null ? estadoAgotada : this.estadoExistenciaId;
        }
    }

    /**
     * Suma contenido por el ajuste de un pesaje, cuando en la realidad hay mas de lo que dice el
     * sistema (logica.md EXISTENCIA-16). Nunca supera lo que cabia al abrirla. Es la unica
     * operacion que revive una existencia agotada (EXISTENCIA-15): vuelve al estado abierta que
     * indica quien la rellena y pierde su fecha de acabado.
     */
    public void rellenar(BigDecimal cantidad, EstadoExistenciaId estadoAbierta) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad a rellenar de la existencia debe ser mayor a cero");
        }

        BigDecimal nuevo = this.contenidoActual.add(cantidad).setScale(ESCALA, RoundingMode.HALF_UP);
        if (nuevo.compareTo(this.contenidoInicial) > 0) {
            throw DomainException.throwException(409,
                    "El ajuste supera el contenido inicial de la existencia con id '" + this.id.getValue() + "'");
        }

        boolean estabaAgotada = isAgotada();
        this.contenidoActual = nuevo;
        if (estabaAgotada) {
            this.fechaAcabado = null;
            this.estadoExistenciaId = estadoAbierta != null ? estadoAbierta : this.estadoExistenciaId;
        }
    }

    /** Lo que aun cabe hasta el contenido inicial. */
    public BigDecimal espacioDisponible() {
        return this.contenidoInicial.subtract(this.contenidoActual);
    }

    public boolean isAgotada() {
        return this.contenidoActual.signum() == 0;
    }

    /** Vencida si su caducidad es anterior a la fecha dada. Sin caducidad, nunca vence. */
    public boolean estaVencida(LocalDate fecha) {
        return this.fechaVencimiento != null && this.fechaVencimiento.isBefore(fecha);
    }

    /**
     * Baja logica. Solo de una existencia agotada: con contenido, primero hay que darlo de baja
     * con un pesaje (merma) para que la perdida quede en la trazabilidad (logica.md EXISTENCIA-16).
     */
    public void eliminar(){
        if (!isAgotada()) {
            throw DomainException.throwException(409,
                    "La existencia aun tiene contenido; registra su pesaje antes de darla de baja");
        }
        this.borrado = true;
    }

    public ExistenciaId getId() {return id;}
    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public DetalleLoteId getDetalleLoteId() {return detalleLoteId;}
    public EstadoExistenciaId getEstadoExistenciaId() {return estadoExistenciaId;}
    public UnidadId getUnidadId() {return unidadId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public boolean isBorrado() {return borrado;}
}
