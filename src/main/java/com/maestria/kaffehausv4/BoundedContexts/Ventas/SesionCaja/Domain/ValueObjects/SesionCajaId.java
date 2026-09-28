package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects;

public class SesionCajaId {
    private final String value;

    public SesionCajaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
