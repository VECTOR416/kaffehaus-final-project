package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de Categoria. */
class CategoriaTest {

    private static final String ID = "ca000000-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Categoria categoria = Categoria.crear("Cafes", true);

        // Assert
        assertNull(categoria.getId());
        assertEquals("Cafes", categoria.getNombre());
        assertTrue(categoria.isEsBorrable());
        assertFalse(categoria.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Categoria categoria = Categoria.restaurar(ID, "Cafes", false, true);

        // Assert
        assertEquals(ID, categoria.getId().getValue());
        assertFalse(categoria.isEsBorrable());
        assertTrue(categoria.isBorrado());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        Categoria categoria = Categoria.restaurar(ID, "Cafes", true, false);

        // Act
        categoria.actualizar("Bebidas", null);
        boolean borrableTrasNombre = categoria.isEsBorrable();
        categoria.actualizar(null, false);

        // Assert
        assertTrue(borrableTrasNombre);
        assertEquals("Bebidas", categoria.getNombre());
        assertFalse(categoria.isEsBorrable());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        Categoria categoria = Categoria.restaurar(ID, "Cafes", true, false);

        // Act
        categoria.eliminar();

        // Assert
        assertTrue(categoria.isBorrado());
        assertEquals("Cafes", categoria.getNombre());
    }

    @Test
    void unaCategoriaNoBorrableNoSeDaDeBaja() {
        // Arrange
        Categoria categoria = Categoria.restaurar(ID, "Cafes", false, false);

        // Act
        DomainException error = assertThrows(DomainException.class, categoria::eliminar);

        // Assert
        assertEquals(409, error.getCodigo());
        assertFalse(categoria.isBorrado());
    }
}
