package com.maestria.kaffehausv4.Shared.Domain.Security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Identidad del usuario que realiza la peticion, expresada en lenguaje de dominio.
 *
 * Es lo que la capa Application recibe a traves de IContextoDeSeguridad: un Value
 * Object inmutable, sin una sola linea de Spring Security. Que el token sea JWT,
 * opaco o SAML es irrelevante aqui, igual que Entidad no sabe que existe
 * EntidadesJpaModel.
 *
 * Esta es la identidad del TOKEN. La identidad de NEGOCIO (agregado Usuario y su
 * Persona asociada) se resuelve en el Bounded Context Usuario a partir del
 * KeycloakId que expone esta clase.
 */
public class UsuarioAutenticado {

    private final KeycloakId id;
    private final String username;
    private final String correo;
    private final Set<RolDeSistema> roles;
    private final Set<String> rolesNoReconocidos;

    private UsuarioAutenticado(
        KeycloakId id,
        String username,
        String correo,
        Set<RolDeSistema> roles,
        Set<String> rolesNoReconocidos
    ) {
        this.id = id;
        this.username = username;
        this.correo = correo;
        this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles));
        this.rolesNoReconocidos = Collections.unmodifiableSet(new LinkedHashSet<>(rolesNoReconocidos));
    }

    public static UsuarioAutenticado crear(
        String id,
        String username,
        String correo,
        Set<RolDeSistema> roles,
        Set<String> rolesNoReconocidos
    ) {
        return new UsuarioAutenticado(
            new KeycloakId(id),
            username,
            correo,
            roles == null ? Collections.emptySet() : roles,
            rolesNoReconocidos == null ? Collections.emptySet() : rolesNoReconocidos
        );
    }

    public boolean tieneRol(RolDeSistema rol) {
        return rol != null && roles.contains(rol);
    }

    public KeycloakId getId() {return id;}
    public String getUsername() {return username;}
    public String getCorreo() {return correo;}

    /** Roles reconocidos por el contrato de autorizacion del codigo. */
    public Set<RolDeSistema> getRoles() {return roles;}

    /**
     * Roles presentes en el token que no figuran en RolDeSistema: creados por el CRUD
     * de Rol y sin regla de autorizacion asociada. Se conservan para trazabilidad.
     */
    public Set<String> getRolesNoReconocidos() {return rolesNoReconocidos;}
}
