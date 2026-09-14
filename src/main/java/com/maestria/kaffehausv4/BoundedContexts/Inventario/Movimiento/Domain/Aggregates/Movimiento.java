package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;

public class Movimiento {
    private MovimientoId id;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private ExistenciaId existenciaId;
    private TipoMovimientoId tipoMovimientoId;
    private boolean borrado;

    private Movimiento(
        MovimientoId id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.existenciaId = existenciaId;
        this.tipoMovimientoId = tipoMovimientoId;
        this.borrado = borrado;
    }

    public static Movimiento restaurar(
        String id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        boolean borrado
    ){
        return new Movimiento(new MovimientoId(id), cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, borrado);
    }

    public static Movimiento crear(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId
    ){
        return new Movimiento(null, cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, false);
    }

    public void actualizar(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId
    ){
        this.cantidad = cantidad != null ? cantidad : this.cantidad;
        this.fechaMovimiento = fechaMovimiento != null ? fechaMovimiento : this.fechaMovimiento;
        this.motivo = motivo != null ? motivo : this.motivo;
        this.existenciaId = existenciaId != null ? existenciaId : this.existenciaId;
        this.tipoMovimientoId = tipoMovimientoId != null ? tipoMovimientoId : this.tipoMovimientoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public MovimientoId getId() {return id;}
    public void setId(MovimientoId id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {this.fechaMovimiento = fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
    public ExistenciaId getExistenciaId() {return existenciaId;}
    public void setExistenciaId(ExistenciaId existenciaId) {this.existenciaId = existenciaId;}
    public TipoMovimientoId getTipoMovimientoId() {return tipoMovimientoId;}
    public void setTipoMovimientoId(TipoMovimientoId tipoMovimientoId) {this.tipoMovimientoId = tipoMovimientoId;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
