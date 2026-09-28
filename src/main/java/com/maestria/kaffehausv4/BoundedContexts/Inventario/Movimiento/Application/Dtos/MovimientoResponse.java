package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;

public class MovimientoResponse {
    private String id;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private String idExistencia;
    private TipoMovimientoResponse tipoMovimiento;
    private String idUsuario;
    private String idProduccion;
    private boolean borrado;

    private MovimientoResponse(
        String id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        String idExistencia,
        TipoMovimientoResponse tipoMovimiento,
        String idUsuario,
        String idProduccion,
        boolean borrado
    ) {
        this.id = id;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.idExistencia = idExistencia;
        this.tipoMovimiento = tipoMovimiento;
        this.idUsuario = idUsuario;
        this.idProduccion = idProduccion;
        this.borrado = borrado;
    }

    public static MovimientoResponse createFromAggregate(Movimiento movimiento, TipoMovimiento tipoMovimiento){
        TipoMovimientoResponse tipoMovimientoResponse = tipoMovimiento != null ? TipoMovimientoResponse.createFromAggregate(tipoMovimiento) : null;
        return new MovimientoResponse(
            movimiento.getId().getValue(),
            movimiento.getCantidad(),
            movimiento.getFechaMovimiento(),
            movimiento.getMotivo(),
            movimiento.getExistenciaId() != null ? movimiento.getExistenciaId().getValue() : null,
            tipoMovimientoResponse,
            movimiento.getUsuarioId() != null ? movimiento.getUsuarioId().getValue() : null,
            movimiento.getProduccionId() != null ? movimiento.getProduccionId().getValue() : null,
            movimiento.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public String getIdExistencia() {return idExistencia;}
    public TipoMovimientoResponse getTipoMovimiento() {return tipoMovimiento;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdProduccion() {return idProduccion;}
    public boolean isBorrado() {return borrado;}
}
