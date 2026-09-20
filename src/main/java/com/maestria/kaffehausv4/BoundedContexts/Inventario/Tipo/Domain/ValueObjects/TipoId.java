package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects;

public class TipoId {
    private final String value;

    public TipoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
