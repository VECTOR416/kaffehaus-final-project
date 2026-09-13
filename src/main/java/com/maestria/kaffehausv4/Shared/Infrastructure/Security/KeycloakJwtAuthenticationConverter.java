package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Construye el Authentication final a partir del token ya validado.
 *
 * Envuelve a KeycloakRolesConverter y fija el nombre del principal en
 * 'preferred_username': por defecto Spring usaria 'sub', que es un UUID ilegible
 * en logs y trazas.
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    static final String CLAIM_EMAIL = "email";

    private final KeycloakRolesConverter rolesConverter;

    public KeycloakJwtAuthenticationConverter(KeycloakRolesConverter rolesConverter) {
        this.rolesConverter = rolesConverter;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = rolesConverter.convert(jwt);
        String nombre = resolverNombre(jwt);

        return new JwtAuthenticationToken(jwt, authorities, nombre);
    }

    private String resolverNombre(Jwt jwt) {
        String username = jwt.getClaimAsString(CLAIM_PREFERRED_USERNAME);
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return jwt.getSubject();
    }
}
