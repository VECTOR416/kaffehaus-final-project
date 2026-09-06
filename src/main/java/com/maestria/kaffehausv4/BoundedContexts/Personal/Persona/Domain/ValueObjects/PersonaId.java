package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects;

public class PersonaId {
private final String value;

    public PersonaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
