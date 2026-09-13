package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Application.Security.IContextoDeSeguridad;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Security.KeycloakId;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Adaptador del puerto IContextoDeSeguridad.
 *
 * Es el UNICO punto del sistema que toca SecurityContextHolder. Traduce el JWT a
 * Value Objects de dominio para que ningun caso de uso tenga que importar Spring
 * Security, igual que EntidadRepositoryImpl traduce entre el dominio y JPA.
 */
@Component
public class ContextoDeSeguridadImpl implements IContextoDeSeguridad {

    @Override
    public boolean hayUsuarioAutenticado() {
        return obtenerTokenDelContexto().isPresent();
    }

    @Override
    public KeycloakId obtenerIdDeUsuarioActual() {
        Jwt jwt = exigirToken();
        return new KeycloakId(jwt.getSubject());
    }

    @Override
    public UsuarioAutenticado obtenerUsuarioActual() {
        Jwt jwt = exigirToken();

        Set<RolDeSistema> roles = new LinkedHashSet<>();
        Set<String> rolesNoReconocidos = new LinkedHashSet<>();

        for (String nombreRol : KeycloakRolesConverter.extraerRolesDeRealm(jwt)) {
            Optional<RolDeSistema> rol = RolDeSistema.desdeNombre(nombreRol);
            if (rol.isPresent()) {
                roles.add(rol.get());
            } else {
                rolesNoReconocidos.add(nombreRol);
            }
        }

        return UsuarioAutenticado.crear(
            jwt.getSubject(),
            jwt.getClaimAsString(KeycloakJwtAuthenticationConverter.CLAIM_PREFERRED_USERNAME),
            jwt.getClaimAsString(KeycloakJwtAuthenticationConverter.CLAIM_EMAIL),
            roles,
            rolesNoReconocidos
        );
    }

    @Override
    public boolean tieneRol(RolDeSistema rol) {
        if (rol == null) return false;

        Optional<Jwt> jwt = obtenerTokenDelContexto();
        if (jwt.isEmpty()) return false;

        return KeycloakRolesConverter.extraerRolesDeRealm(jwt.get()).contains(rol.name());
    }

    private Jwt exigirToken() {
        return obtenerTokenDelContexto()
                .orElseThrow(() -> AutorizacionException.throwException(
                        401, "No hay un usuario autenticado en el contexto"));
    }

    private Optional<Jwt> obtenerTokenDelContexto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken)) {
            return Optional.empty();
        }

        if (!authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.of(((JwtAuthenticationToken) authentication).getToken());
    }
}
