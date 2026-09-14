package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects;

public class DetalleLoteId {
    private final String value;

    public DetalleLoteId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
