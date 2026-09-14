package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class Existencia {
    private ExistenciaId id;
    private BigDecimal contenidoInicial;
    private BigDecimal contenidoActual;
    private LocalDateTime fechaAcabado;
    private LocalDate fechaVencimiento;
    private DetalleLoteId detalleLoteId;
    private EstadoExistenciaId estadoExistenciaId;
    private UnidadId unidadId;
    private boolean borrado;

    private Existencia(
        ExistenciaId id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        boolean borrado
    ){
        this.id = id;
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.detalleLoteId = detalleLoteId;
        this.estadoExistenciaId = estadoExistenciaId;
        this.unidadId = unidadId;
        this.borrado = borrado;
    }

    public static Existencia restaurar(
        String id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId,
        boolean borrado
    ){
        return new Existencia(new ExistenciaId(id), contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, detalleLoteId, estadoExistenciaId, unidadId, borrado);
    }

    public static Existencia crear(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId
    ){
        return new Existencia(null, contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, detalleLoteId, estadoExistenciaId, unidadId, false);
    }

    public void actualizar(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        DetalleLoteId detalleLoteId,
        EstadoExistenciaId estadoExistenciaId,
        UnidadId unidadId
    ){
        this.contenidoInicial = contenidoInicial != null ? contenidoInicial : this.contenidoInicial;
        this.contenidoActual = contenidoActual != null ? contenidoActual : this.contenidoActual;
        this.fechaAcabado = fechaAcabado != null ? fechaAcabado : this.fechaAcabado;
        this.fechaVencimiento = fechaVencimiento != null ? fechaVencimiento : this.fechaVencimiento;
        this.detalleLoteId = detalleLoteId != null ? detalleLoteId : this.detalleLoteId;
        this.estadoExistenciaId = estadoExistenciaId != null ? estadoExistenciaId : this.estadoExistenciaId;
        this.unidadId = unidadId != null ? unidadId : this.unidadId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public ExistenciaId getId() {return id;}
    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public DetalleLoteId getDetalleLoteId() {return detalleLoteId;}
    public EstadoExistenciaId getEstadoExistenciaId() {return estadoExistenciaId;}
    public UnidadId getUnidadId() {return unidadId;}
    public boolean isBorrado() {return borrado;}
}
