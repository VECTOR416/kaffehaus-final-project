package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects;

public class CategoriaId {
    private final String value;

    public CategoriaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
