package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects;

public class TipoMovimientoId {
    private final String value;

    public TipoMovimientoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
