package com.maestria.kaffehausv4.Shared.Presentation.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/** Pruebas unitarias de la identidad que viaja en el token y de su envoltorio de Spring Security. */
class UsuarioAutenticadoTest {

    private static UsuarioAutenticado conRol(String rol) {
        return new UsuarioAutenticado("u-1", "Ana Rojas", "r-1", rol);
    }

    @Test
    void tieneAlgunRolComparaSinImportarMayusculas() {
        // Arrange
        UsuarioAutenticado usuario = conRol("Administrador");

        // Act y Assert
        assertTrue(usuario.tieneAlgunRol("ADMINISTRADOR"));
        assertTrue(usuario.tieneAlgunRol("Operador", "administrador"));
    }

    @Test
    void tieneAlgunRolEsFalsoSiNingunoCoincide() {
        // Arrange
        UsuarioAutenticado usuario = conRol("Operador");

        // Act y Assert
        assertFalse(usuario.tieneAlgunRol("Administrador"));
        assertFalse(usuario.tieneAlgunRol());
        assertFalse(usuario.tieneAlgunRol((String) null));
    }

    @Test
    void unUsuarioSinRolNoTieneNinguno() {
        // Arrange
        UsuarioAutenticado usuario = conRol(null);

        // Act
        boolean tiene = usuario.tieneAlgunRol("Administrador");

        // Assert
        assertFalse(tiene);
    }

    @Test
    void securityUserExponeLosDatosDelUsuario() {
        // Arrange
        UsuarioAutenticado usuario = conRol("Administrador");

        // Act
        SecurityUser securityUser = new SecurityUser(usuario);

        // Assert
        assertEquals("u-1", securityUser.getUsuarioId());
        assertEquals("Ana Rojas", securityUser.getNombreCompleto());
        assertEquals("r-1", securityUser.getRolId());
        assertEquals("Administrador", securityUser.getRolNombre());
        assertNull(securityUser.getPassword());
    }

    @Test
    void laAutoridadEsElRolEnMayusculasConPrefijoYSinEspacios() {
        // Arrange
        SecurityUser securityUser = new SecurityUser(conRol(" Jefe de turno "));

        // Act
        String autoridad = securityUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElseThrow();

        // Assert
        assertEquals("ROLE_JEFE_DE_TURNO", autoridad);
    }

    @Test
    void sinRolLaAutoridadQuedaSoloConElPrefijo() {
        // Arrange
        SecurityUser securityUser = new SecurityUser(conRol(null));

        // Act
        String autoridad = securityUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElseThrow();

        // Assert
        assertEquals("ROLE_", autoridad);
    }
}
