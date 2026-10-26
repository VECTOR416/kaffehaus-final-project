package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de Unidad. */
class UnidadTest {

    private static final String ID = "b0000000-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Unidad unidad = Unidad.crear("Kilogramo", "kg", false);

        // Assert
        assertNull(unidad.getId());
        assertEquals("Kilogramo", unidad.getUnidad());
        assertEquals("kg", unidad.getAbreviatura());
        assertFalse(unidad.isEsBorrable());
        assertFalse(unidad.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Unidad unidad = Unidad.restaurar(ID, "Gramo", "g", true, true);

        // Assert
        assertEquals(ID, unidad.getId().getValue());
        assertEquals("g", unidad.getAbreviatura());
        assertTrue(unidad.isEsBorrable());
        assertTrue(unidad.isBorrado());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        Unidad unidad = Unidad.restaurar(ID, "Gramo", "g", false, false);

        // Act
        unidad.actualizar(null, "gr", null);

        // Assert
        assertEquals("Gramo", unidad.getUnidad());
        assertEquals("gr", unidad.getAbreviatura());
        assertFalse(unidad.isEsBorrable());
    }

    @Test
    void actualizarPuedeCambiarTodosLosCampos() {
        // Arrange
        Unidad unidad = Unidad.restaurar(ID, "Gramo", "g", false, false);

        // Act
        unidad.actualizar("Litro", "l", true);

        // Assert
        assertEquals("Litro", unidad.getUnidad());
        assertEquals("l", unidad.getAbreviatura());
        assertTrue(unidad.isEsBorrable());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        Unidad unidad = Unidad.restaurar(ID, "Gramo", "g", true, false);

        // Act
        unidad.eliminar();

        // Assert
        assertTrue(unidad.isBorrado());
    }

    @Test
    void unaUnidadNoBorrableNoSeDaDeBaja() {
        // Arrange
        Unidad unidad = Unidad.restaurar(ID, "Gramo", "g", false, false);

        // Act
        DomainException error = assertThrows(DomainException.class, unidad::eliminar);

        // Assert
        assertEquals(409, error.getCodigo());
        assertFalse(unidad.isBorrado());
    }
}
