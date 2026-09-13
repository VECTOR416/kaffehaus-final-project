package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;

/**
 * Traduce los roles de Keycloak a authorities de Spring Security.
 *
 * POR QUE HACE FALTA: el JwtGrantedAuthoritiesConverter por defecto lee el claim
 * 'scope'/'scp' y genera authorities con prefijo SCOPE_. Keycloak NO publica los
 * roles ahi, sino anidados en 'realm_access.roles'. Sin este conversor,
 * hasRole('ADMINISTRADOR') siempre devuelve false y toda la API responde 403.
 *
 * Es el fallo numero uno al integrar Keycloak con Spring Boot.
 */
@Component
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    static final String CLAIM_REALM_ACCESS = "realm_access";
    static final String CLAIM_ROLES = "roles";

    /**
     * Roles tecnicos que Keycloak asigna por defecto y que no significan nada para el
     * negocio. Se descartan para que el contexto de seguridad no se llene de ruido.
     */
    private static final Set<String> ROLES_TECNICOS = Set.of(
        "OFFLINE_ACCESS",
        "UMA_AUTHORIZATION"
    );

    private static final String PREFIJO_ROLES_POR_DEFECTO = "DEFAULT-ROLES-";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String nombreRol : extraerRolesDeRealm(jwt)) {
            authorities.add(new SimpleGrantedAuthority(
                RolDeSistema.PREFIJO_AUTHORITY + nombreRol
            ));
        }

        return authorities;
    }

    /**
     * Devuelve los nombres de rol ya normalizados (mayusculas) y sin los roles
     * tecnicos de Keycloak. Se expone a nivel de paquete para que
     * ContextoDeSeguridadImpl reconstruya UsuarioAutenticado con la misma logica.
     */
    static List<String> extraerRolesDeRealm(Jwt jwt) {
        List<String> resultado = new ArrayList<>();

        for (String rolCrudo : leerListaDeRoles(jwt)) {
            String normalizado = RolDeSistema.normalizar(rolCrudo);

            if (normalizado == null || normalizado.isEmpty()) continue;
            if (ROLES_TECNICOS.contains(normalizado)) continue;
            if (normalizado.startsWith(PREFIJO_ROLES_POR_DEFECTO)) continue;

            resultado.add(normalizado);
        }

        return resultado;
    }

    @SuppressWarnings("unchecked")
    private static List<String> leerListaDeRoles(Jwt jwt) {
        if (jwt == null) return Collections.emptyList();

        Object realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);
        if (!(realmAccess instanceof Map)) return Collections.emptyList();

        Object roles = ((Map<String, Object>) realmAccess).get(CLAIM_ROLES);
        if (!(roles instanceof Collection)) return Collections.emptyList();

        List<String> textos = new ArrayList<>();
        for (Object rol : (Collection<Object>) roles) {
            if (rol instanceof String) {
                textos.add((String) rol);
            }
        }

        return textos;
    }
}
