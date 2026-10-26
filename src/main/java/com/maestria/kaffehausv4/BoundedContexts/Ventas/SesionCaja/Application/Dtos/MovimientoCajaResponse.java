package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;

public class MovimientoCajaResponse {
    private String id;
    private String idUsuario;
    private String tipo;
    private BigDecimal monto;
    private String motivo;
    private LocalDateTime fecha;
    private String idVenta;

    private MovimientoCajaResponse(String id, String idUsuario, String tipo, BigDecimal monto, String motivo, LocalDateTime fecha, String idVenta) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.fecha = fecha;
        this.idVenta = idVenta;
    }

    public static MovimientoCajaResponse createFromAggregate(MovimientoCaja movimiento) {
        return new MovimientoCajaResponse(
            movimiento.getId() != null ? movimiento.getId().getValue() : null,
            movimiento.getUsuarioId().getValue(),
            movimiento.getTipo(),
            movimiento.getMonto(),
            movimiento.getMotivo(),
            movimiento.getFecha(),
            movimiento.getVentaId() != null ? movimiento.getVentaId().getValue() : null
        );
    }

    public String getId() {return id;}
    public String getIdUsuario() {return idUsuario;}
    public String getTipo() {return tipo;}
    public BigDecimal getMonto() {return monto;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}
    public String getIdVenta() {return idVenta;}
}
