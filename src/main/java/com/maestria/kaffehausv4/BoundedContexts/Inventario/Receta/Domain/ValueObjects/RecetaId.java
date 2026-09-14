package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects;

public class RecetaId {
    private final String value;

    public RecetaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
