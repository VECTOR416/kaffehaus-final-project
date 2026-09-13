package com.maestria.kaffehausv4.Shared.Domain.Security;

/**
 * Identificador del usuario en el proveedor de identidad (claim 'sub' del token).
 *
 * NO es el mismo valor que 'usuario.id' ni que 'usuario.id_persona' de la base de
 * datos local: es el puente entre ambos mundos, y se persiste en 'usuario.keycloak_id'.
 *
 * Value Object inmutable, sin anotaciones ni dependencias de framework, con la misma
 * forma que EntidadId, PersonaId o RolId.
 */
public class KeycloakId {
    private final String value;

    public KeycloakId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeycloakId)) return false;
        KeycloakId otro = (KeycloakId) obj;
        return value == null ? otro.value == null : value.equals(otro.value);
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
