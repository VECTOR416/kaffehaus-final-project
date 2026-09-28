package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas del agregado Venta (ventas.md). Son de dominio puro: no levantan Spring ni tocan la
 * base, porque las invariantes que se verifican aqui no dependen de ninguna de las dos.
 */
class VentaTest {

    private static final ProductoId PRODUCTO_CAFE = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b1111");
    private static final ProductoId PRODUCTO_TE = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b2222");
    private static final EstadoVentaId COMPLETADA = new EstadoVentaId("22222222-2222-2222-2222-222222222222");
    private static final EstadoVentaId ANULADA = new EstadoVentaId("33333333-3333-3333-3333-333333333333");
    private static final UsuarioId CAJERO = new UsuarioId("11111111-1111-1111-1111-111111111111");
    private static final String ID_LINEA_CAFE = "44444444-4444-4444-4444-444444444444";
    private static final ClienteId MOSTRADOR = new ClienteId("00000000-0000-0000-0000-000000000001");
    private static final SesionCajaId CAJA = new SesionCajaId("66666666-6666-6666-6666-666666666666");
    private static final TipoPagoId EFECTIVO = new TipoPagoId("77777777-7777-7777-7777-777777777777");
    private static final TipoPagoId TARJETA = new TipoPagoId("88888888-8888-8888-8888-888888888888");

    private Venta ventaVacia() {
        return Venta.crear(LocalDateTime.of(2026, 9, 17, 10, 30), CAJERO, MOSTRADOR, CAJA, COMPLETADA, null);
    }

    private Venta ventaRegistrada() {
        return Venta.restaurar(
                "55555555-5555-5555-5555-555555555555",
                1L,
                LocalDateTime.of(2026, 9, 17, 10, 30),
                new BigDecimal("25.00"),
                CAJERO,
                MOSTRADOR,
                CAJA,
                COMPLETADA,
                null,
                null,
                null,
                false,
                List.of(DetalleVenta.restaurar(ID_LINEA_CAFE, PRODUCTO_CAFE,
                        new BigDecimal("12.50"), new BigDecimal("2"), new BigDecimal("25.00"), BigDecimal.ZERO,
                        new BigDecimal("0.0000"), true, false)),
                List.of());
    }

    @Test
    void unaVentaNuevaNaceCompletadaSinDetallesYConTotalEnCero() {
        Venta venta = ventaVacia();

        assertTrue(venta.getDetalles().isEmpty());
        assertEquals(new BigDecimal("0.00"), venta.getTotal());
        assertTrue(venta.estaEn(COMPLETADA));
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
        DomainException error = assertThrows(DomainException.class, ventaVacia()::validarRegistrable);

        assertEquals(400, error.getCodigo());
    }

    @Test
    void registrarElCostoDeUnaLineaLoSumaAlCostoDeLaVenta() {
        Venta venta = ventaRegistrada();

        venta.registrarCosto(ID_LINEA_CAFE, new BigDecimal("3.4567"));

        assertEquals(new BigDecimal("3.4567"), venta.getCostoTotal());
        assertEquals(new BigDecimal("25.00"), venta.getTotal());
    }

    @Test
    void anularConservaLasLineasYRegistraMotivoYUsuario() {
        Venta venta = ventaRegistrada();

        venta.anular(COMPLETADA, ANULADA, CAJERO, "Cobro duplicado");

        assertTrue(venta.estaEn(ANULADA));
        assertEquals("Cobro duplicado", venta.getMotivoAnulacion());
        assertEquals(CAJERO.getValue(), venta.getUsuarioAnulacionId().getValue());
        assertEquals(1, venta.getDetallesActivos().size());
        assertEquals(new BigDecimal("25.00"), venta.getTotal());
    }

    @Test
    void unaVentaAnuladaNoSeVuelveAAnularNiCambia() {
        Venta venta = ventaRegistrada();
        venta.anular(COMPLETADA, ANULADA, CAJERO, "Cobro duplicado");

        DomainException otraVez = assertThrows(DomainException.class, () -> venta.anular(COMPLETADA, ANULADA, CAJERO, "Otra"));
        DomainException cliente = assertThrows(DomainException.class,
                () -> venta.asignarCliente(new ClienteId("66666666-6666-6666-6666-666666666666"), ANULADA));

        assertEquals(409, otraVez.getCodigo());
        assertEquals(409, cliente.getCodigo());
    }

    @Test
    void anularExigeMotivo() {
        DomainException error = assertThrows(DomainException.class, () -> ventaRegistrada().anular(COMPLETADA, ANULADA, CAJERO, " "));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unaVentaNoSeDaDeBaja() {
        Venta venta = ventaRegistrada();

        DomainException error = assertThrows(DomainException.class, venta::eliminar);

        assertEquals(409, error.getCodigo());
        assertTrue(!venta.isBorrado());
    }

    @Test
    void elMotivoDeSusMovimientosCitaLaVenta() {
        assertEquals("Venta '55555555-5555-5555-5555-555555555555'", ventaRegistrada().motivoDeMovimientos());
    }

    // ------------------------------------------------------------------ CAJ-01, CAJ-02

    @Test
    void unaVentaExigeClienteYSesionDeCaja() {
        LocalDateTime ahora = LocalDateTime.of(2026, 9, 28, 9, 0);

        DomainException sinCliente = assertThrows(DomainException.class,
                () -> Venta.crear(ahora, CAJERO, null, CAJA, COMPLETADA, null));
        DomainException sinCaja = assertThrows(DomainException.class,
                () -> Venta.crear(ahora, CAJERO, MOSTRADOR, null, COMPLETADA, null));

        assertEquals(400, sinCliente.getCodigo());
        assertEquals(409, sinCaja.getCodigo());
    }

    // ------------------------------------------------------------------ CAJ-10..CAJ-13

    @Test
    void losPagosMixtosDebenSumarElTotalYElEfectivoDaCambio() {
        Venta venta = ventaVacia();
        venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), new BigDecimal("2"), BigDecimal.ZERO);

        venta.agregarPago(EFECTIVO, true, new BigDecimal("15.00"), new BigDecimal("20.00"), null);
        venta.agregarPago(TARJETA, false, new BigDecimal("10.00"), null, "VISA-1234");
        venta.validarPagos();

        assertEquals(new BigDecimal("25.00"), venta.getTotalPagado());
        assertEquals(new BigDecimal("5.00"), venta.getCambio());
        assertEquals(new BigDecimal("0.00"), venta.getPagosActivos().get(1).getCambio());
        assertEquals(new BigDecimal("10.00"), venta.getPagosActivos().get(1).getMontoRecibido());
    }

    @Test
    void rechazaPagosQueNoSumanElTotal() {
        Venta venta = ventaVacia();
        venta.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("12.50"), new BigDecimal("2"), BigDecimal.ZERO);
        venta.agregarPago(EFECTIVO, true, new BigDecimal("20.00"), null, null);

        DomainException error = assertThrows(DomainException.class, venta::validarPagos);

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("25.00"));
    }

    @Test
    void elEfectivoRecibidoDebeCubrirElMontoYSoloElEfectivoDaCambio() {
        Venta venta = ventaVacia();

        DomainException insuficiente = assertThrows(DomainException.class,
                () -> venta.agregarPago(EFECTIVO, true, new BigDecimal("10.00"), new BigDecimal("5.00"), null));
        DomainException tarjetaConCambio = assertThrows(DomainException.class,
                () -> venta.agregarPago(TARJETA, false, new BigDecimal("10.00"), new BigDecimal("20.00"), null));
        DomainException montoCero = assertThrows(DomainException.class,
                () -> venta.agregarPago(EFECTIVO, true, BigDecimal.ZERO, null, null));

        assertEquals(400, insuficiente.getCodigo());
        assertEquals(400, tarjetaConCambio.getCodigo());
        assertEquals(400, montoCero.getCodigo());
        assertTrue(venta.getPagos().isEmpty());
    }
}
