package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de una linea de venta (VEN-13, VEN-14, VEN-23). */
class DetalleVentaTest {

    private static final ProductoId CAFE = new ProductoId("66666666-6666-6666-6666-666666666666");

    private static DetalleVenta linea(String precio, String cantidad, String descuento) {
        return DetalleVenta.crear(CAFE, new BigDecimal(precio), new BigDecimal(cantidad),
                descuento != null ? new BigDecimal(descuento) : null);
    }

    @Test
    void crearCalculaElSubtotalComoPrecioPorCantidadMenosDescuento() {
        // Act
        DetalleVenta detalle = linea("12.50", "3", "2.50");

        // Assert
        assertNull(detalle.getId());
        assertSame(CAFE, detalle.getProductoId());
        assertEquals(new BigDecimal("12.50"), detalle.getPrecioUnitario());
        assertEquals(new BigDecimal("35.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("2.50"), detalle.getDescuentoAplicado());
        assertTrue(detalle.isEstado());
        assertFalse(detalle.isBorrado());
    }

    @Test
    void sinDescuentoElSubtotalEsElImporteBruto() {
        // Act
        DetalleVenta detalle = linea("8", "2", null);

        // Assert
        assertEquals(new BigDecimal("16.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("0.00"), detalle.getDescuentoAplicado());
    }

    @Test
    void elSubtotalSeRedondeaADosDecimales() {
        // Act
        DetalleVenta detalle = linea("3.333", "3", null);

        // Assert
        assertEquals(new BigDecimal("10.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("3.33"), detalle.getPrecioUnitario());
    }

    @Test
    void elDescuentoPuedeIgualarElImportePeroNoSuperarlo() {
        // Act
        DetalleVenta regalado = linea("10", "2", "20");
        DomainException error = assertThrows(DomainException.class, () -> linea("10", "2", "20.01"));

        // Assert
        assertEquals(new BigDecimal("0.00"), regalado.getSubtotal());
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearExigeUnProducto() {
        // Act
        DomainException sinProducto = assertThrows(DomainException.class,
                () -> DetalleVenta.crear(null, BigDecimal.ONE, BigDecimal.ONE, null));
        DomainException sinId = assertThrows(DomainException.class,
                () -> DetalleVenta.crear(new ProductoId(null), BigDecimal.ONE, BigDecimal.ONE, null));

        // Assert
        assertEquals(400, sinProducto.getCodigo());
        assertEquals(400, sinId.getCodigo());
    }

    @Test
    void crearRechazaUnPrecioNegativoPeroAceptaCero() {
        // Act
        DetalleVenta gratis = linea("0", "1", null);
        DomainException error = assertThrows(DomainException.class, () -> linea("-0.01", "1", null));
        DomainException porNulo = assertThrows(DomainException.class,
                () -> DetalleVenta.crear(CAFE, null, BigDecimal.ONE, null));

        // Assert
        assertEquals(new BigDecimal("0.00"), gratis.getSubtotal());
        assertEquals(400, error.getCodigo());
        assertEquals(400, porNulo.getCodigo());
    }

    @Test
    void crearRechazaUnaCantidadQueNoEsPositiva() {
        // Act
        DomainException porCero = assertThrows(DomainException.class, () -> linea("10", "0", null));
        DomainException porNegativa = assertThrows(DomainException.class, () -> linea("10", "-1", null));
        DomainException porNula = assertThrows(DomainException.class,
                () -> DetalleVenta.crear(CAFE, BigDecimal.TEN, null, null));

        // Assert
        assertEquals(400, porCero.getCodigo());
        assertEquals(400, porNegativa.getCodigo());
        assertEquals(400, porNula.getCodigo());
    }

    @Test
    void crearRechazaUnDescuentoNegativo() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> linea("10", "1", "-1"));

        // Assert
        assertEquals(400, error.getCodigo());
    }

}
