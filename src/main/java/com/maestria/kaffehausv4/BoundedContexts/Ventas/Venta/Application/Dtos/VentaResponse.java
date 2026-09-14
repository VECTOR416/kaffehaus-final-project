package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

public class VentaResponse {
    private String id;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private String idUsuario;
    private String idCliente;
    private EstadoVentaResponse estadoVenta;

    private VentaResponse(
        String id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        String idUsuario,
        String idCliente,
        EstadoVentaResponse estadoVenta
    ) {
        this.id = id;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.estadoVenta = estadoVenta;
    }

    public static VentaResponse createFromAggregate(Venta venta, EstadoVenta estadoVenta){
        EstadoVentaResponse estadoVentaResponse = estadoVenta != null ? EstadoVentaResponse.createFromAggregate(estadoVenta) : null;
        return new VentaResponse(
            venta.getId().getValue(),
            venta.getFechaVenta(),
            venta.getTotal(),
            venta.getUsuarioId() != null ? venta.getUsuarioId().getValue() : null,
            venta.getClienteId() != null ? venta.getClienteId().getValue() : null,
            estadoVentaResponse
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public void setFechaVenta(LocalDateTime fechaVenta) {this.fechaVenta = fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public String getIdUsuario() {return idUsuario;}
    public void setIdUsuario(String idUsuario) {this.idUsuario = idUsuario;}
    public String getIdCliente() {return idCliente;}
    public void setIdCliente(String idCliente) {this.idCliente = idCliente;}
    public EstadoVentaResponse getEstadoVenta() {return estadoVenta;}
    public void setEstadoVenta(EstadoVentaResponse estadoVenta) {this.estadoVenta = estadoVenta;}
}
