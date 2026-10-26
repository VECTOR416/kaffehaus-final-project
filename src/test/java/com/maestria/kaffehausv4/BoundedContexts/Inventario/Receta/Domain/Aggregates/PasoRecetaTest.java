package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de un paso de receta. */
class PasoRecetaTest {

    @Test
    void crearLimpiaLaDescripcionYTomaLaPosicionIndicada() {
        // Act
        PasoReceta paso = PasoReceta.crear(2, "  Amasar diez minutos  ");

        // Assert
        assertNull(paso.getId());
        assertEquals(2, paso.getPasos());
        assertEquals("Amasar diez minutos", paso.getDescripcion());
        assertFalse(paso.isBorrado());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void crearExigeUnaDescripcion(String descripcion) {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> PasoReceta.crear(1, descripcion));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void actualizarDescripcionCambiaElTextoSinMoverElPaso() {
        // Arrange
        PasoReceta paso = PasoReceta.crear(3, "Hornear");

        // Act
        paso.actualizarDescripcion(" Hornear a 180 grados ");

        // Assert
        assertEquals("Hornear a 180 grados", paso.getDescripcion());
        assertEquals(3, paso.getPasos());
    }

    @Test
    void actualizarDescripcionConNuloConservaElTextoYVaciaLoRechaza() {
        // Arrange
        PasoReceta paso = PasoReceta.crear(1, "Mezclar");

        // Act
        paso.actualizarDescripcion(null);
        DomainException error = assertThrows(DomainException.class, () -> paso.actualizarDescripcion("  "));

        // Assert
        assertEquals("Mezclar", paso.getDescripcion());
        assertEquals(400, error.getCodigo());
    }

    @Test
    void reubicarEnCambiaSoloElNumeroDePaso() {
        // Arrange
        PasoReceta paso = PasoReceta.crear(1, "Mezclar");

        // Act
        paso.reubicarEn(4);

        // Assert
        assertEquals(4, paso.getPasos());
        assertEquals("Mezclar", paso.getDescripcion());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        PasoReceta paso = PasoReceta.restaurar("99999999-9999-9999-9999-999999999999", 1, "Mezclar", false);

        // Act
        paso.eliminar();

        // Assert
        assertTrue(paso.isBorrado());
        assertEquals("99999999-9999-9999-9999-999999999999", paso.getId().getValue());
    }
}
