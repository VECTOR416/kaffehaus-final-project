package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

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
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "En revision", false);

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
        EstadoExistencia estado = EstadoExistencia.restaurar(ID, "En revision", false);

        // Act
        estado.eliminar();

        // Assert
        assertTrue(estado.isBorrado());
        assertEquals("En revision", estado.getNombre());
    }

    @Test
    void unaFilaSemanticaNoSeRenombraNiSeDaDeBaja() {
        // Arrange (logica.md GENERAL-19)
        EstadoExistencia semantica = EstadoExistencia.restaurar(ID, "Abierta", false);

        // Act
        DomainException renombrar = assertThrows(DomainException.class, () -> semantica.actualizar("Otro nombre"));
        DomainException darDeBaja = assertThrows(DomainException.class, semantica::eliminar);
        semantica.actualizar("ABIERTA");

        // Assert
        assertEquals(409, renombrar.getCodigo());
        assertEquals(409, darDeBaja.getCodigo());
        assertEquals("Abierta", semantica.getNombre());
        assertFalse(semantica.isBorrado());
        assertTrue(semantica.esSemantico());
        assertFalse(EstadoExistencia.restaurar(ID, "En revision", false).esSemantico());
    }
}
