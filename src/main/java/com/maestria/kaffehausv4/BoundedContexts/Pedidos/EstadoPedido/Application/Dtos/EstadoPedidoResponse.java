package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;

public class EstadoPedidoResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private EstadoPedidoResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoPedidoResponse createFromAggregate(EstadoPedido estadoPedido){
        return new EstadoPedidoResponse(
            estadoPedido.getId().getValue(),
            estadoPedido.getNombre(),
            estadoPedido.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
