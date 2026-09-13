package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;
import com.maestria.kaffehausv4.Shared.Presentation.GlobalExceptionHandler;

import tools.jackson.databind.json.JsonMapper;

/**
 * Verificaciones sin contexto de Spring ni base de datos de las piezas de seguridad.
 *
 * Cubre en particular la serializacion del error con Jackson 3 (tools.jackson), que
 * es la API que trae Spring Boot 4: Jackson 2 (com.fasterxml) no esta en el classpath
 * y usarlo compilaria solo si se anadiera una dependencia sobrante, fallando despues
 * en tiempo de ejecucion al no existir el bean.
 */
class SeguridadKeycloakTest {

    private static final String SUB = "a3f1c2e4-0000-0000-0000-000000009b7d";

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Jwt tokenDePrueba(List<String> rolesDeRealm) {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .subject(SUB)
                .claim("preferred_username", "fichazo")
                .claim("email", "usuario@ejemplo.com")
                .claim("realm_access", Map.of("roles", rolesDeRealm))
                .build();
    }

    @Test
    @DisplayName("El error de seguridad se serializa con el mismo formato que GlobalExceptionHandler")
    void serializaElErrorConJackson3() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        GlobalExceptionHandler.ErrorResponse body = new GlobalExceptionHandler.ErrorResponse(
                401, "Authorization Exception", "Token de acceso ausente o invalido", OffsetDateTime.now());

        String json = jsonMapper.writeValueAsString(body);

        assertThat(json)
                .contains("\"codigo\":401")
                .contains("\"tipo\":\"Authorization Exception\"")
                .contains("\"mensaje\":\"Token de acceso ausente o invalido\"")
                .contains("\"timestamp\"");
    }

    @Test
    @DisplayName("realm_access.roles se convierte en authorities con prefijo ROLE_")
    void convierteLosRolesDelRealm() {
        KeycloakRolesConverter converter = new KeycloakRolesConverter();

        Collection<GrantedAuthority> authorities = converter.convert(
                tokenDePrueba(List.of("ADMINISTRADOR", "EMPLEADO")));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMINISTRADOR", "ROLE_EMPLEADO");
    }

    @Test
    @DisplayName("Los roles tecnicos de Keycloak se descartan")
    void descartaLosRolesTecnicos() {
        KeycloakRolesConverter converter = new KeycloakRolesConverter();

        Collection<GrantedAuthority> authorities = converter.convert(
                tokenDePrueba(List.of("offline_access", "uma_authorization", "default-roles-kaffehaus", "CAJERO")));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CAJERO");
    }

    @Test
    @DisplayName("El contexto expone el sub como KeycloakId y separa los roles no reconocidos")
    void exponeLaIdentidadDelToken() {
        Jwt jwt = tokenDePrueba(List.of("ADMINISTRADOR", "BARISTA_SENIOR"));
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(), "fichazo"));

        ContextoDeSeguridadImpl contexto = new ContextoDeSeguridadImpl();

        assertThat(contexto.hayUsuarioAutenticado()).isTrue();
        assertThat(contexto.obtenerIdDeUsuarioActual().getValue()).isEqualTo(SUB);
        assertThat(contexto.tieneRol(RolDeSistema.ADMINISTRADOR)).isTrue();
        assertThat(contexto.tieneRol(RolDeSistema.CAJERO)).isFalse();

        UsuarioAutenticado usuario = contexto.obtenerUsuarioActual();
        assertThat(usuario.getUsername()).isEqualTo("fichazo");
        assertThat(usuario.getCorreo()).isEqualTo("usuario@ejemplo.com");
        assertThat(usuario.getRoles()).containsExactly(RolDeSistema.ADMINISTRADOR);
        // Rol creado por el CRUD de Rol: se conserva, pero no otorga permisos.
        assertThat(usuario.getRolesNoReconocidos()).containsExactly("BARISTA_SENIOR");
    }

    @Test
    @DisplayName("Sin autenticacion se lanza AutorizacionException 401 y tieneRol devuelve false")
    void exigeUsuarioAutenticado() {
        ContextoDeSeguridadImpl contexto = new ContextoDeSeguridadImpl();

        assertThat(contexto.hayUsuarioAutenticado()).isFalse();
        assertThat(contexto.tieneRol(RolDeSistema.ADMINISTRADOR)).isFalse();

        org.assertj.core.api.Assertions
                .assertThatThrownBy(contexto::obtenerIdDeUsuarioActual)
                .isInstanceOf(com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException.class)
                .hasMessageContaining("No hay un usuario autenticado");
    }
}
