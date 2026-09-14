package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;

public class EstadoPedidoResponse {
    private String id;
    private String nombre;

    private EstadoPedidoResponse(
        String id,
        String nombre
    ) {
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoPedidoResponse createFromAggregate(EstadoPedido estadoPedido){
        return new EstadoPedidoResponse(
            estadoPedido.getId().getValue(),
            estadoPedido.getNombre()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
