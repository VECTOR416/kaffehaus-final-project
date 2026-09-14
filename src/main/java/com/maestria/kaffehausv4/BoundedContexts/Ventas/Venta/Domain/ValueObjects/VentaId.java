package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects;

public class VentaId {
    private final String value;

    public VentaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
