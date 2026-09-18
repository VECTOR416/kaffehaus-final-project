package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas del agregado Venta. Son de dominio puro: no levantan Spring ni tocan la base,
 * porque las invariantes que se verifican aqui no dependen de ninguna de las dos.
 */
class VentaTest {

    private static final ProductoId PRODUCTO_CAFE = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b1111");
    private static final ProductoId PRODUCTO_TE = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b2222");

    private Venta ventaVacia() {
        return Venta.crear(
                LocalDateTime.of(2026, 9, 17, 10, 30),
                new UsuarioId("11111111-1111-1111-1111-111111111111"),
                null,
                new EstadoVentaId("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void unaVentaNuevaNaceSinDetallesYConTotalEnCero() {
        Venta venta = ventaVacia();

        assertTrue(venta.getDetalles().isEmpty());
        assertEquals(new BigDecimal("0.00"), venta.getTotal());
    }

    @Test
    void agregarDetalleCalculaElSubtotalYAcumulaElTotal() {
        Venta venta = ventaVacia();

        venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), new BigDecimal("2"), BigDecimal.ZERO);
        venta.agregarDetalle(PRODUCTO_TE, new BigDecimal("8.00"), new BigDecimal("3"), new BigDecimal("4.00"));

        List<DetalleVenta> detalles = venta.getDetalles();
        assertEquals(2, detalles.size());
        assertEquals(new BigDecimal("25.00"), detalles.get(0).getSubtotal());
        assertEquals(new BigDecimal("20.00"), detalles.get(1).getSubtotal());
        assertEquals(new BigDecimal("45.00"), venta.getTotal());
    }

    @Test
    void removerDetalleRecalculaElTotal() {
        Venta venta = Venta.restaurar(
                "33333333-3333-3333-3333-333333333333",
                LocalDateTime.of(2026, 9, 17, 10, 30),
                new BigDecimal("45.00"),
                new UsuarioId("11111111-1111-1111-1111-111111111111"),
                null,
                new EstadoVentaId("22222222-2222-2222-2222-222222222222"),
                List.of(
                        DetalleVenta.restaurar("44444444-4444-4444-4444-444444444444", PRODUCTO_CAFE,
                                new BigDecimal("12.50"), new BigDecimal("2"), new BigDecimal("25.00"), BigDecimal.ZERO, true),
                        DetalleVenta.restaurar("55555555-5555-5555-5555-555555555555", PRODUCTO_TE,
                                new BigDecimal("8.00"), new BigDecimal("3"), new BigDecimal("20.00"), new BigDecimal("4.00"), true)));

        venta.removerDetalle("44444444-4444-4444-4444-444444444444");

        assertEquals(1, venta.getDetalles().size());
        assertEquals(new BigDecimal("20.00"), venta.getTotal());
    }

    @Test
    void removerUnDetalleAjenoALaVentaFalla() {
        Venta venta = ventaVacia();

        DomainException error = assertThrows(DomainException.class,
                () -> venta.removerDetalle("99999999-9999-9999-9999-999999999999"));

        assertEquals(404, error.getCodigo());
    }

    @Test
    void laColeccionDeDetallesNoSePuedeMutarDesdeFuera() {
        Venta venta = ventaVacia();
        venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), new BigDecimal("2"), BigDecimal.ZERO);

        List<DetalleVenta> detalles = venta.getDetalles();

        assertThrows(UnsupportedOperationException.class, () -> detalles.remove(0));
    }

    @Test
    void rechazaUnaCantidadNoPositiva() {
        Venta venta = ventaVacia();

        DomainException error = assertThrows(DomainException.class,
                () -> venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertEquals(400, error.getCodigo());
        assertTrue(venta.getDetalles().isEmpty());
        assertEquals(new BigDecimal("0.00"), venta.getTotal());
    }

    @Test
    void rechazaUnDescuentoMayorAlImporteDeLaLinea() {
        Venta venta = ventaVacia();

        DomainException error = assertThrows(DomainException.class,
                () -> venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("10.00"), new BigDecimal("1"), new BigDecimal("15.00")));

        assertEquals(400, error.getCodigo());
        assertTrue(venta.getDetalles().isEmpty());
    }

    @Test
    void unaVentaSinDetallesNoEsRegistrable() {
        Venta venta = ventaVacia();

        DomainException error = assertThrows(DomainException.class, venta::validarRegistrable);

        assertEquals(400, error.getCodigo());
    }

    @Test
    void actualizarNoTocaElTotalDerivado() {
        Venta venta = ventaVacia();
        venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), new BigDecimal("2"), BigDecimal.ZERO);

        venta.actualizar(LocalDateTime.of(2026, 9, 18, 9, 0), null, null, null);

        assertEquals(new BigDecimal("25.00"), venta.getTotal());
        assertEquals(LocalDateTime.of(2026, 9, 18, 9, 0), venta.getFechaVenta());
    }
}
