package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects;

public class EntidadId {
    private final String value;

    public EntidadId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
