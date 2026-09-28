package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects;

public class MovimientoCajaId {
    private final String value;

    public MovimientoCajaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
