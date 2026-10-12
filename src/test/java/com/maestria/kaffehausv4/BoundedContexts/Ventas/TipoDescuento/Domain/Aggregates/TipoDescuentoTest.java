package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de dominio puro de TipoDescuento: catalogo de tipos de descuento y su semantica. */
class TipoDescuentoTest {

    private static final String ID = "c0000007-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        TipoDescuento tipo = TipoDescuento.crear("Porcentaje");

        // Assert
        assertNull(tipo.getId());
        assertEquals("Porcentaje", tipo.getNombre());
        assertFalse(tipo.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        TipoDescuento tipo = TipoDescuento.restaurar(ID, "Porcentaje", true);

        // Assert
        assertEquals(ID, tipo.getId().getValue());
        assertEquals("Porcentaje", tipo.getNombre());
        assertTrue(tipo.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        TipoDescuento tipo = TipoDescuento.restaurar(ID, "Porcentaje", false);

        // Act
        tipo.actualizar("Monto fijo");

        // Assert
        assertEquals("Monto fijo", tipo.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        TipoDescuento tipo = TipoDescuento.restaurar(ID, "Porcentaje", false);

        // Act
        tipo.actualizar(null);

        // Assert
        assertEquals("Porcentaje", tipo.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        TipoDescuento tipo = TipoDescuento.restaurar(ID, "Porcentaje", false);

        // Act
        tipo.eliminar();

        // Assert
        assertTrue(tipo.isBorrado());
        assertEquals("Porcentaje", tipo.getNombre());
    }

    @ParameterizedTest
    @CsvSource({
            "Porcentaje, true, false, true",
            "porcentaje, true, false, true",
            "Monto fijo, false, true, true",
            "MONTO FIJO, false, true, true",
            "2x1, false, false, false"})
    void laSemanticaSeReconocePorNombreSinImportarMayusculas(String nombre, boolean esPorcentaje, boolean esMontoFijo, boolean esAplicable) {
        // Arrange
        TipoDescuento tipo = TipoDescuento.crear(nombre);

        // Act y Assert
        assertEquals(esPorcentaje, tipo.esPorcentaje());
        assertEquals(esMontoFijo, tipo.esMontoFijo());
        assertEquals(esAplicable, tipo.esAplicable());
    }

    @Test
    void unTipoSinNombreNoEsAplicable() {
        // Arrange
        TipoDescuento tipo = TipoDescuento.crear(null);

        // Act
        boolean aplicable = tipo.esAplicable();

        // Assert
        assertFalse(aplicable);
    }
}
