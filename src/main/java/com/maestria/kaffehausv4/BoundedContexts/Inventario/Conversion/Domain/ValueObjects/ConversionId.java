package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects;

public class ConversionId {
    private final String value;

    public ConversionId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
