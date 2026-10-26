package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects;

public class TipoPagoId {
    private final String value;

    public TipoPagoId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
