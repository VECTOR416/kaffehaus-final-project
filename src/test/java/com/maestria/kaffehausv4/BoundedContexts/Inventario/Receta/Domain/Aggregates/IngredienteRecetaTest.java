package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de un ingrediente de receta. */
class IngredienteRecetaTest {

    private static final ProductoId HARINA = new ProductoId("77777777-7777-7777-7777-777777777777");

    @Test
    void crearGuardaLaCantidadACuatroDecimales() {
        // Act
        IngredienteReceta ingrediente = IngredienteReceta.crear(HARINA, new BigDecimal("1.23456"));

        // Assert
        assertNull(ingrediente.getId());
        assertSame(HARINA, ingrediente.getProductoId());
        assertEquals(new BigDecimal("1.2346"), ingrediente.getCantidad());
        assertFalse(ingrediente.isBorrado());
    }

    @Test
    void crearExigeUnProducto() {
        // Act
        DomainException sinProducto = assertThrows(DomainException.class, () -> IngredienteReceta.crear(null, BigDecimal.ONE));
        DomainException sinId = assertThrows(DomainException.class, () -> IngredienteReceta.crear(new ProductoId(null), BigDecimal.ONE));

        // Assert
        assertEquals(400, sinProducto.getCodigo());
        assertEquals(400, sinId.getCodigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void crearRechazaUnaCantidadQueNoEsPositiva(String cantidad) {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> IngredienteReceta.crear(HARINA, new BigDecimal(cantidad)));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearRechazaUnaCantidadNula() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> IngredienteReceta.crear(HARINA, null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void actualizarCantidadCambiaElValorYConNuloLoConserva() {
        // Arrange
        IngredienteReceta ingrediente = IngredienteReceta.crear(HARINA, new BigDecimal("2"));

        // Act
        ingrediente.actualizarCantidad(null);
        BigDecimal trasNulo = ingrediente.getCantidad();
        ingrediente.actualizarCantidad(new BigDecimal("3.5"));

        // Assert
        assertEquals(new BigDecimal("2.0000"), trasNulo);
        assertEquals(new BigDecimal("3.5000"), ingrediente.getCantidad());
    }

    @Test
    void actualizarCantidadRechazaUnValorNoPositivoSinAlterarElActual() {
        // Arrange
        IngredienteReceta ingrediente = IngredienteReceta.crear(HARINA, new BigDecimal("2"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> ingrediente.actualizarCantidad(BigDecimal.ZERO));

        // Assert
        assertEquals(400, error.getCodigo());
        assertEquals(new BigDecimal("2.0000"), ingrediente.getCantidad());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        IngredienteReceta ingrediente = IngredienteReceta.restaurar("88888888-8888-8888-8888-888888888888", HARINA, BigDecimal.ONE, false);

        // Act
        ingrediente.eliminar();

        // Assert
        assertTrue(ingrediente.isBorrado());
        assertEquals("88888888-8888-8888-8888-888888888888", ingrediente.getId().getValue());
    }
}
