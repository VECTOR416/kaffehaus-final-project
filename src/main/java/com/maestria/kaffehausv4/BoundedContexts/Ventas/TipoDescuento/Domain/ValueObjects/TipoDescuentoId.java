package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects;

public class TipoDescuentoId {
    private final String value;

    public TipoDescuentoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
