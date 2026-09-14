package com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class Pedido {
    private PedidoId id;
    private LocalDateTime fechaEntrega;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private VentaId ventaId;
    private EstadoPedidoId estadoId;

    private Pedido(
        PedidoId id,
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId
    ){
        this.id = id;
        this.fechaEntrega = fechaEntrega;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.ventaId = ventaId;
        this.estadoId = estadoId;
    }

    public static Pedido restaurar(
        String id,
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId
    ){
        return new Pedido(new PedidoId(id), fechaEntrega, latitud, longitud, descripcion, ventaId, estadoId);
    }

    public static Pedido crear(
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId
    ){
        return new Pedido(null, fechaEntrega, latitud, longitud, descripcion, ventaId, estadoId);
    }

    public void actualizar(
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId
    ){
        this.fechaEntrega = fechaEntrega != null ? fechaEntrega : this.fechaEntrega;
        this.latitud = latitud != null ? latitud : this.latitud;
        this.longitud = longitud != null ? longitud : this.longitud;
        this.descripcion = descripcion != null ? descripcion : this.descripcion;
        this.ventaId = ventaId != null ? ventaId : this.ventaId;
        this.estadoId = estadoId != null ? estadoId : this.estadoId;
    }

    public PedidoId getId() {return id;}
    public void setId(PedidoId id) {this.id = id;}
    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public void setFechaEntrega(LocalDateTime fechaEntrega) {this.fechaEntrega = fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public void setLatitud(BigDecimal latitud) {this.latitud = latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public void setLongitud(BigDecimal longitud) {this.longitud = longitud;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public VentaId getVentaId() {return ventaId;}
    public void setVentaId(VentaId ventaId) {this.ventaId = ventaId;}
    public EstadoPedidoId getEstadoId() {return estadoId;}
    public void setEstadoId(EstadoPedidoId estadoId) {this.estadoId = estadoId;}
}
