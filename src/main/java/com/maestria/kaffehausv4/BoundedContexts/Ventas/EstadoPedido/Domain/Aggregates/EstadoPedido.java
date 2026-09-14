package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;

public class EstadoPedido {
    private EstadoPedidoId id;
    private String nombre;

    private EstadoPedido(
        EstadoPedidoId id,
        String nombre
    ){
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoPedido restaurar(
        String id,
        String nombre
    ){
        return new EstadoPedido(new EstadoPedidoId(id), nombre);
    }

    public static EstadoPedido crear(
        String nombre
    ){
        return new EstadoPedido(null, nombre);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public EstadoPedidoId getId() {return id;}
    public void setId(EstadoPedidoId id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
