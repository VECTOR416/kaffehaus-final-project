package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects;

public class MovimientoId {
    private final String value;

    public MovimientoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
