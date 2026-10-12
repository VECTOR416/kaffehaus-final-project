package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de dominio puro de TipoPago: catalogo de formas de pago. */
class TipoPagoTest {

    private static final String ID = "c0000008-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        TipoPago tipo = TipoPago.crear("Efectivo");

        // Assert
        assertNull(tipo.getId());
        assertEquals("Efectivo", tipo.getNombre());
        assertFalse(tipo.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        TipoPago tipo = TipoPago.restaurar(ID, "Efectivo", true);

        // Assert
        assertEquals(ID, tipo.getId().getValue());
        assertEquals("Efectivo", tipo.getNombre());
        assertTrue(tipo.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        TipoPago tipo = TipoPago.restaurar(ID, "Efectivo", false);

        // Act
        tipo.actualizar("Tarjeta");

        // Assert
        assertEquals("Tarjeta", tipo.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        TipoPago tipo = TipoPago.restaurar(ID, "Efectivo", false);

        // Act
        tipo.actualizar(null);

        // Assert
        assertEquals("Efectivo", tipo.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        TipoPago tipo = TipoPago.restaurar(ID, "Efectivo", false);

        // Act
        tipo.eliminar();

        // Assert
        assertTrue(tipo.isBorrado());
        assertEquals("Efectivo", tipo.getNombre());
    }

    @ParameterizedTest
    @CsvSource({"Efectivo, true", "EFECTIVO, true", "efectivo, true", "Tarjeta, false", "QR, false"})
    void soloElEfectivoSeReconoceComoTal(String nombre, boolean esperado) {
        // Arrange
        TipoPago tipo = TipoPago.crear(nombre);

        // Act
        boolean esEfectivo = tipo.esEfectivo();

        // Assert
        assertEquals(esperado, esEfectivo);
    }

    @Test
    void unTipoSinNombreNoEsEfectivo() {
        // Arrange
        TipoPago tipo = TipoPago.crear(null);

        // Act
        boolean esEfectivo = tipo.esEfectivo();

        // Assert
        assertFalse(esEfectivo);
    }
}
