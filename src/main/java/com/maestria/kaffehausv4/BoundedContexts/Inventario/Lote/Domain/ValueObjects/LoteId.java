package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects;

public class LoteId {
    private final String value;

    public LoteId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
