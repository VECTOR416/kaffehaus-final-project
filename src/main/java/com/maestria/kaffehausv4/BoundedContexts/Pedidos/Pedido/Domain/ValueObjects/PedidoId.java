package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects;

public class PedidoId {
    private final String value;

    public PedidoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
