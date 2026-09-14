package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects;

public class EstadoPedidoId {
    private final String value;

    public EstadoPedidoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
