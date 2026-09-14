package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects;

public class ExistenciaId {
    private final String value;

    public ExistenciaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
