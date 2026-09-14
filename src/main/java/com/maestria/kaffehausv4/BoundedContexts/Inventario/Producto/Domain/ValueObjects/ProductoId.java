package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects;

public class ProductoId {
    private final String value;

    public ProductoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
