package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de dominio puro de Entidad (la empresa de un proveedor). */
class EntidadTest {

    private static final String ID = "e0000000-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Entidad entidad = Entidad.crear("Tostadora Andina", "1020304050", "70000000", "Cafe en grano");

        // Assert
        assertNull(entidad.getId());
        assertEquals("Tostadora Andina", entidad.getNombre());
        assertEquals("1020304050", entidad.getNit());
        assertEquals("70000000", entidad.getTelefono());
        assertEquals("Cafe en grano", entidad.getDescripcion());
        assertFalse(entidad.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Entidad entidad = Entidad.restaurar(ID, "Tostadora Andina", "1020304050", null, null, true);

        // Assert
        assertEquals(ID, entidad.getId().getValue());
        assertNull(entidad.getTelefono());
        assertTrue(entidad.isBorrado());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        Entidad entidad = Entidad.restaurar(ID, "Tostadora Andina", "1020304050", "70000000", "Cafe", false);

        // Act
        entidad.actualizar(null, "999", null, "Cafe y cacao");

        // Assert
        assertEquals("Tostadora Andina", entidad.getNombre());
        assertEquals("999", entidad.getNit());
        assertEquals("70000000", entidad.getTelefono());
        assertEquals("Cafe y cacao", entidad.getDescripcion());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        Entidad entidad = Entidad.restaurar(ID, "Tostadora Andina", "1020304050", null, null, false);

        // Act
        entidad.eliminar();

        // Assert
        assertTrue(entidad.isBorrado());
        assertEquals("Tostadora Andina", entidad.getNombre());
    }
}
