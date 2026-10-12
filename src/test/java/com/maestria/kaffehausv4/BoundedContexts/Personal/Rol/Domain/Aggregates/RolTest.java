package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de dominio puro de Rol: catalogo de roles. */
class RolTest {

    private static final String ID = "c0000005-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Rol rol = Rol.crear("Administrador");

        // Assert
        assertNull(rol.getId());
        assertEquals("Administrador", rol.getNombre());
        assertFalse(rol.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Rol rol = Rol.restaurar(ID, "Administrador", true);

        // Assert
        assertEquals(ID, rol.getId().getValue());
        assertEquals("Administrador", rol.getNombre());
        assertTrue(rol.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Administrador", false);

        // Act
        rol.actualizar("Operador");

        // Assert
        assertEquals("Operador", rol.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Administrador", false);

        // Act
        rol.actualizar(null);

        // Assert
        assertEquals("Administrador", rol.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Administrador", false);

        // Act
        rol.eliminar();

        // Assert
        assertTrue(rol.isBorrado());
        assertEquals("Administrador", rol.getNombre());
    }
}
