package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de dominio puro de EstadoExistencia: catalogo de estados de una existencia. */
class EstadoExistenciaTest {

    private static final String ID = "c0000001-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        EstadoExistencia estado = EstadoExistencia.crear("Abierta");

        // Assert
        assertNull(estado.getId());
        assertEquals("Abierta", estado.getNombre());
        assertFalse(estado.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "Abierta", true);

        // Assert
        assertEquals(ID, estado.getId().getValue());
        assertEquals("Abierta", estado.getNombre());
        assertTrue(estado.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "Abierta", false);

        // Act
        estado.actualizar("Agotada");

        // Assert
        assertEquals("Agotada", estado.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "Abierta", false);

        // Act
        estado.actualizar(null);

        // Assert
        assertEquals("Abierta", estado.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "Abierta", false);

        // Act
        estado.eliminar();

        // Assert
        assertTrue(estado.isBorrado());
        assertEquals("Abierta", estado.getNombre());
    }
}
