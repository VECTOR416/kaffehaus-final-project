package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects;

public class DetalleVentaId {
    private final String value;

    public DetalleVentaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
