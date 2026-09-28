package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects;

public class PagoVentaId {
    private final String value;

    public PagoVentaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
