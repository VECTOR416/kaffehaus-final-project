package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects;

public class EstadoVentaId {
    private final String value;

    public EstadoVentaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
