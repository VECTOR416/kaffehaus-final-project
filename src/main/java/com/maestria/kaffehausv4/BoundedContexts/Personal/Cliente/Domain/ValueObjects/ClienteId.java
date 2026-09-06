package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects;

public class ClienteId {
private final String value;

    public ClienteId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}