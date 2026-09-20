package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class Movimiento {
    private MovimientoId id;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private ExistenciaId existenciaId;
    private TipoMovimientoId tipoMovimientoId;
    private UsuarioId usuarioId;
    private boolean borrado;

    private Movimiento(
        MovimientoId id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.existenciaId = existenciaId;
        this.tipoMovimientoId = tipoMovimientoId;
        this.usuarioId = usuarioId;
        this.borrado = borrado;
    }

    public static Movimiento restaurar(
        String id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        return new Movimiento(new MovimientoId(id), cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, usuarioId, borrado);
    }

    public static Movimiento crear(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId
    ){
        return new Movimiento(null, cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, usuarioId, false);
    }

    public void actualizar(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId
    ){
        this.cantidad = cantidad != null ? cantidad : this.cantidad;
        this.fechaMovimiento = fechaMovimiento != null ? fechaMovimiento : this.fechaMovimiento;
        this.motivo = motivo != null ? motivo : this.motivo;
        this.existenciaId = existenciaId != null ? existenciaId : this.existenciaId;
        this.tipoMovimientoId = tipoMovimientoId != null ? tipoMovimientoId : this.tipoMovimientoId;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public MovimientoId getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public ExistenciaId getExistenciaId() {return existenciaId;}
    public TipoMovimientoId getTipoMovimientoId() {return tipoMovimientoId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public boolean isBorrado() {return borrado;}
}
