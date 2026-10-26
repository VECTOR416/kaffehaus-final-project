package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;

/** Pruebas de dominio puro de Usuario. */
class UsuarioTest {

    private static final String ID = "d1000000-0000-0000-0000-000000000001";
    private static final String PERSONA = "d1000000-0000-0000-0000-000000000002";
    private static final String ROL = "d1000000-0000-0000-0000-000000000003";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Arrange
        PersonaId persona = new PersonaId(PERSONA);
        RolId rol = new RolId(ROL);

        // Act
        Usuario usuario = Usuario.crear(persona, "ana@kaffe.com", "hash", rol);

        // Assert
        assertNull(usuario.getId());
        assertSame(persona, usuario.getPersonaId());
        assertEquals("ana@kaffe.com", usuario.getCorreo());
        assertEquals("hash", usuario.getContrasena());
        assertSame(rol, usuario.getRolId());
        assertFalse(usuario.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Usuario usuario = Usuario.restaurar(ID, PERSONA, "ana@kaffe.com", "hash", true, new RolId(ROL));

        // Assert
        assertEquals(ID, usuario.getId().getValue());
        assertEquals(PERSONA, usuario.getPersonaId().getValue());
        assertEquals(ROL, usuario.getRolId().getValue());
        assertTrue(usuario.isBorrado());
    }

    @Test
    void actualizarConTodoNuloNoCambiaNada() {
        // Arrange
        Usuario usuario = Usuario.restaurar(ID, PERSONA, "ana@kaffe.com", "hash", false, new RolId(ROL));

        // Act
        usuario.actualizar(null, null, null, null);

        // Assert
        assertEquals(PERSONA, usuario.getPersonaId().getValue());
        assertEquals("ana@kaffe.com", usuario.getCorreo());
        assertEquals("hash", usuario.getContrasena());
        assertEquals(ROL, usuario.getRolId().getValue());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        Usuario usuario = Usuario.restaurar(ID, PERSONA, "ana@kaffe.com", "hash", false, new RolId(ROL));

        // Act
        usuario.actualizar(null, "ana.rojas@kaffe.com", "nuevoHash", "d1000000-0000-0000-0000-000000000009");

        // Assert
        assertEquals(PERSONA, usuario.getPersonaId().getValue());
        assertEquals("ana.rojas@kaffe.com", usuario.getCorreo());
        assertEquals("nuevoHash", usuario.getContrasena());
        assertEquals("d1000000-0000-0000-0000-000000000009", usuario.getRolId().getValue());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        Usuario usuario = Usuario.restaurar(ID, PERSONA, "ana@kaffe.com", "hash", false, new RolId(ROL));

        // Act
        usuario.eliminar();

        // Assert
        assertTrue(usuario.isBorrado());
        assertEquals("ana@kaffe.com", usuario.getCorreo());
    }
}
