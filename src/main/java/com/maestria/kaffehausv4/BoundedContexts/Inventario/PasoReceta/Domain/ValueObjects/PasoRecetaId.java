package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects;

public class PasoRecetaId {
    private final String value;

    public PasoRecetaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
