package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects;

public class IngredienteRecetaId {
    private final String value;

    public IngredienteRecetaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
