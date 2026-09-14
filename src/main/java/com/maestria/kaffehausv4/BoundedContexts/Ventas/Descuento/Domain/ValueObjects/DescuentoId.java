package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects;

public class DescuentoId {
    private final String value;

    public DescuentoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
