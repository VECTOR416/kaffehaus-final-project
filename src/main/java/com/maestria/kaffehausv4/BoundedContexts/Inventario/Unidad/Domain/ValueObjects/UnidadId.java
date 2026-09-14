package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects;

public class UnidadId {
    private final String value;

    public UnidadId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
