package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de EstadoVenta: catalogo de estados de una venta. */
class EstadoVentaTest {

    private static final String ID = "c0000006-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        EstadoVenta estado = EstadoVenta.crear("Completada");

        // Assert
        assertNull(estado.getId());
        assertEquals("Completada", estado.getNombre());
        assertFalse(estado.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        EstadoVenta estado = EstadoVenta.restaurar(ID, "Completada", true);

        // Assert
        assertEquals(ID, estado.getId().getValue());
        assertEquals("Completada", estado.getNombre());
        assertTrue(estado.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        EstadoVenta estado = EstadoVenta.restaurar(ID, "En espera", false);

        // Act
        estado.actualizar("Anulada");

        // Assert
        assertEquals("Anulada", estado.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        EstadoVenta estado = EstadoVenta.restaurar(ID, "Completada", false);

        // Act
        estado.actualizar(null);

        // Assert
        assertEquals("Completada", estado.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        EstadoVenta estado = EstadoVenta.restaurar(ID, "En espera", false);

        // Act
        estado.eliminar();

        // Assert
        assertTrue(estado.isBorrado());
        assertEquals("En espera", estado.getNombre());
    }

    @Test
    void unaFilaSemanticaNoSeRenombraNiSeDaDeBaja() {
        // Arrange (logica.md GENERAL-19)
        EstadoVenta semantica = EstadoVenta.restaurar(ID, "Completada", false);

        // Act
        DomainException renombrar = assertThrows(DomainException.class, () -> semantica.actualizar("Otro nombre"));
        DomainException darDeBaja = assertThrows(DomainException.class, semantica::eliminar);
        semantica.actualizar("COMPLETADA");

        // Assert
        assertEquals(409, renombrar.getCodigo());
        assertEquals(409, darDeBaja.getCodigo());
        assertEquals("Completada", semantica.getNombre());
        assertFalse(semantica.isBorrado());
        assertTrue(semantica.esSemantico());
        assertFalse(EstadoVenta.restaurar(ID, "En espera", false).esSemantico());
    }
}
