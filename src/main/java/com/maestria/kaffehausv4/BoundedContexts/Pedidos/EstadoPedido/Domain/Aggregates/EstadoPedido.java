package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;

public class EstadoPedido {
    private EstadoPedidoId id;
    private String nombre;
    private boolean borrado;

    private EstadoPedido(
        EstadoPedidoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoPedido restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new EstadoPedido(new EstadoPedidoId(id), nombre, borrado);
    }

    public static EstadoPedido crear(
        String nombre
    ){
        return new EstadoPedido(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public EstadoPedidoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
