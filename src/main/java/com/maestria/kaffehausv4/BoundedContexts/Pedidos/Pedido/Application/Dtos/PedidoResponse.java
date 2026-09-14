package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;

public class PedidoResponse {
    private String id;
    private LocalDateTime fechaEntrega;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private String idVenta;
    private EstadoPedidoResponse estado;

    private PedidoResponse(
        String id,
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        String idVenta,
        EstadoPedidoResponse estado
    ) {
        this.id = id;
        this.fechaEntrega = fechaEntrega;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.idVenta = idVenta;
        this.estado = estado;
    }

    public static PedidoResponse createFromAggregate(Pedido pedido, EstadoPedido estado){
        EstadoPedidoResponse estadoResponse = estado != null ? EstadoPedidoResponse.createFromAggregate(estado) : null;
        return new PedidoResponse(
            pedido.getId().getValue(),
            pedido.getFechaEntrega(),
            pedido.getLatitud(),
            pedido.getLongitud(),
            pedido.getDescripcion(),
            pedido.getVentaId() != null ? pedido.getVentaId().getValue() : null,
            estadoResponse
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public void setFechaEntrega(LocalDateTime fechaEntrega) {this.fechaEntrega = fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public void setLatitud(BigDecimal latitud) {this.latitud = latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public void setLongitud(BigDecimal longitud) {this.longitud = longitud;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public String getIdVenta() {return idVenta;}
    public void setIdVenta(String idVenta) {this.idVenta = idVenta;}
    public EstadoPedidoResponse getEstado() {return estado;}
    public void setEstado(EstadoPedidoResponse estado) {this.estado = estado;}
}
