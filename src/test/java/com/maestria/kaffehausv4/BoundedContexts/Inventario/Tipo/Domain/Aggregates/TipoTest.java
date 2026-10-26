package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de dominio puro de Tipo: catalogo de tipos de producto. */
class TipoTest {

    private static final String ID = "c0000002-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Tipo tipo = Tipo.crear("Producto");

        // Assert
        assertNull(tipo.getId());
        assertEquals("Producto", tipo.getNombre());
        assertFalse(tipo.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Tipo tipo = Tipo.restaurar(ID, "Producto", true);

        // Assert
        assertEquals(ID, tipo.getId().getValue());
        assertEquals("Producto", tipo.getNombre());
        assertTrue(tipo.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        Tipo tipo = Tipo.restaurar(ID, "Producto", false);

        // Act
        tipo.actualizar("Insumo");

        // Assert
        assertEquals("Insumo", tipo.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        Tipo tipo = Tipo.restaurar(ID, "Producto", false);

        // Act
        tipo.actualizar(null);

        // Assert
        assertEquals("Producto", tipo.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        Tipo tipo = Tipo.restaurar(ID, "Producto", false);

        // Act
        tipo.eliminar();

        // Assert
        assertTrue(tipo.isBorrado());
        assertEquals("Producto", tipo.getNombre());
    }
}
