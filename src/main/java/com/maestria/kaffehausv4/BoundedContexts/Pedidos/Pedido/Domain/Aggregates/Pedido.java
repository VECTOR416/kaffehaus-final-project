package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class Pedido {
    private PedidoId id;
    private LocalDateTime fechaEntrega;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private VentaId ventaId;
    private EstadoPedidoId estadoId;
    private boolean borrado;

    private Pedido(
        PedidoId id,
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId,
        boolean borrado
    ){
        this.id = id;
        this.fechaEntrega = fechaEntrega;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.ventaId = ventaId;
        this.estadoId = estadoId;
        this.borrado = borrado;
    }

    public static Pedido restaurar(
        String id,
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId,
        boolean borrado
    ){
        return new Pedido(new PedidoId(id), fechaEntrega, latitud, longitud, descripcion, ventaId, estadoId, borrado);
    }

    public static Pedido crear(
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        VentaId ventaId,
        EstadoPedidoId estadoId
    ){
        return new Pedido(null, fechaEntrega, latitud, longitud, descripcion, ventaId, estadoId, false);
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

    public void eliminar(){
        this.borrado = true;
    }

    public PedidoId getId() {return id;}
    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public String getDescripcion() {return descripcion;}
    public VentaId getVentaId() {return ventaId;}
    public EstadoPedidoId getEstadoId() {return estadoId;}
    public boolean isBorrado() {return borrado;}
}
