package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects;

public class UsuarioId {
    private final String value;

    public UsuarioId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
