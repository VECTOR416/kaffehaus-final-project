package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects;

public class ProduccionId {
    private final String value;

    public ProduccionId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
