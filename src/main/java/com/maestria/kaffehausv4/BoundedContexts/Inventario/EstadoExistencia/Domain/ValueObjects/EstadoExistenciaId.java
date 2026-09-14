package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects;

public class EstadoExistenciaId {
    private final String value;

    public EstadoExistenciaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
