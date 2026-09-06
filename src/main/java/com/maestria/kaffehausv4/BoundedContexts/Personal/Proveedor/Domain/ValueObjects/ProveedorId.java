package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects;

public class ProveedorId {
    private final String value;

    public ProveedorId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
