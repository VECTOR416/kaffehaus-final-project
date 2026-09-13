package com.maestria.kaffehausv4.Shared.Domain.Security;

import java.util.Optional;

/**
 * Catalogo de roles que el CODIGO referencia en las reglas de autorizacion.
 *
 * Distincion importante: el CRUD de Rol puede crear roles adicionales en tiempo de
 * ejecucion, pero un rol que no figure aqui no otorga ningun permiso hasta que una
 * regla de SecurityConfig lo mencione. Este enum es el contrato de autorizacion;
 * la tabla 'rol' es el catalogo de negocio.
 *
 * Los nombres deben coincidir caracter a caracter con los realm roles de Keycloak
 * y con la columna 'rol.nombre'.
 */
public enum RolDeSistema {
    ADMINISTRADOR,
    SUPERVISOR,
    EMPLEADO,
    CAJERO;

    /** Prefijo que Spring Security antepone internamente al evaluar hasRole(). */
    public static final String PREFIJO_AUTHORITY = "ROLE_";

    /**
     * Traduce el nombre de un rol externo al enum. Devuelve vacio si el rol no forma
     * parte del contrato de autorizacion (rol tecnico de Keycloak o rol creado por CRUD).
     */
    public static Optional<RolDeSistema> desdeNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizado = normalizar(nombre);

        for (RolDeSistema rol : values()) {
            if (rol.name().equals(normalizado)) {
                return Optional.of(rol);
            }
        }

        return Optional.empty();
    }

    /** Normaliza un nombre de rol al formato canonico: mayusculas, sin espacios sobrantes. */
    public static String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim().toUpperCase().replace(' ', '_');
    }

    /** Representacion que espera Spring Security: ROLE_ADMINISTRADOR. */
    public String comoAuthority() {
        return PREFIJO_AUTHORITY + name();
    }
}
