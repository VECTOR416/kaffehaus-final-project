package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas del agregado Lote. Son de dominio puro: no levantan Spring ni tocan la base, porque
 * las invariantes de inventario que se verifican aqui no dependen de ninguna de las dos.
 */
class LoteTest {

    private static final ProductoId PRODUCTO_CAFE = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b1111");
    private static final ProductoId PRODUCTO_AZUCAR = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b2222");
    private static final String ID_DETALLE_CAFE = "44444444-4444-4444-4444-444444444444";
    private static final String ID_DETALLE_AZUCAR = "55555555-5555-5555-5555-555555555555";

    private Lote loteVacio() {
        return Lote.crear(
                LocalDateTime.of(2026, 9, 17, 10, 30),
                new ProveedorId("11111111-1111-1111-1111-111111111111"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"));
    }

    private Lote lotePersistido() {
        return Lote.restaurar(
                "33333333-3333-3333-3333-333333333333",
                LocalDateTime.of(2026, 9, 17, 10, 30),
                new BigDecimal("160.00"),
                new ProveedorId("11111111-1111-1111-1111-111111111111"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"),
                false,
                null,
                false,
                List.of(
                        DetalleLote.restaurar(ID_DETALLE_CAFE, PRODUCTO_CAFE,
                                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("12.00"), false,
                                LocalDate.of(2027, 1, 31), false),
                        DetalleLote.restaurar(ID_DETALLE_AZUCAR, PRODUCTO_AZUCAR,
                                new BigDecimal("8.00"), new BigDecimal("8.00"), new BigDecimal("5.00"), true,
                                null, false)));
    }

    @Test
    void unLoteNuevoNaceSinDetallesYConTotalEnCero() {
        Lote lote = loteVacio();

        assertTrue(lote.getDetalles().isEmpty());
        assertEquals(new BigDecimal("0.00"), lote.getTotal());
    }

    @Test
    void agregarDetalleAcumulaElCostoDeLaCompra() {
        Lote lote = loteVacio();

        lote.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("10"), new BigDecimal("12.00"), false, LocalDate.of(2027, 1, 31));
        lote.agregarDetalle(PRODUCTO_AZUCAR, new BigDecimal("8"), new BigDecimal("5.00"), true, null);

        assertEquals(2, lote.getDetallesActivos().size());
        assertEquals(new BigDecimal("160.00"), lote.getTotal());
    }

    @Test
    void laCantidadRestanteNaceIgualALaCantidadComprada() {
        Lote lote = loteVacio();

        lote.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("10"), new BigDecimal("12.00"), false, LocalDate.of(2027, 1, 31));

        DetalleLote detalle = lote.getDetallesActivos().get(0);
        assertEquals(detalle.getCantidadInicial(), detalle.getCantidadRestante());
    }

    @Test
    void ajustarCantidadRestanteNoAlteraElCostoDelLote() {
        Lote lote = lotePersistido();

        lote.ajustarCantidadRestante(ID_DETALLE_CAFE, new BigDecimal("3.5"));

        assertEquals(new BigDecimal("3.50"), lote.getDetallesActivos().get(0).getCantidadRestante());
        assertEquals(new BigDecimal("160.00"), lote.getTotal());
    }

    @Test
    void rechazaUnaCantidadRestanteMayorALaComprada() {
        Lote lote = lotePersistido();

        DomainException error = assertThrows(DomainException.class,
                () -> lote.ajustarCantidadRestante(ID_DETALLE_CAFE, new BigDecimal("11")));

        assertEquals(400, error.getCodigo());
        assertEquals(new BigDecimal("10.00"), lote.getDetallesActivos().get(0).getCantidadRestante());
    }

    @Test
    void rechazaUnaCantidadRestanteNegativa() {
        Lote lote = lotePersistido();

        DomainException error = assertThrows(DomainException.class,
                () -> lote.ajustarCantidadRestante(ID_DETALLE_CAFE, new BigDecimal("-1")));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void rechazaBajarLaCantidadInicialPorDebajoDeLoQueQuedaEnAlmacen() {
        Lote lote = lotePersistido();
        lote.ajustarCantidadRestante(ID_DETALLE_CAFE, new BigDecimal("7"));

        DomainException error = assertThrows(DomainException.class,
                () -> lote.actualizarDetalle(ID_DETALLE_CAFE, new BigDecimal("5"), null, null, null));

        assertEquals(400, error.getCodigo());
        assertEquals(new BigDecimal("10.00"), lote.getDetallesActivos().get(0).getCantidadInicial());
    }

    @Test
    void rechazaUnaCantidadInicialNoPositiva() {
        Lote lote = loteVacio();

        DomainException error = assertThrows(DomainException.class,
                () -> lote.agregarDetalle(PRODUCTO_CAFE, BigDecimal.ZERO, new BigDecimal("12.00"), false, null));

        assertEquals(400, error.getCodigo());
        assertTrue(lote.getDetalles().isEmpty());
        assertEquals(new BigDecimal("0.00"), lote.getTotal());
    }

    @Test
    void rechazaUnPrecioNegativo() {
        Lote lote = loteVacio();

        DomainException error = assertThrows(DomainException.class,
                () -> lote.agregarDetalle(PRODUCTO_CAFE, new BigDecimal("10"), new BigDecimal("-1.00"), false, null));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void removerDetalleEsBajaLogicaYDescuentaSuCosto() {
        Lote lote = lotePersistido();

        lote.removerDetalle(ID_DETALLE_CAFE);

        assertEquals(1, lote.getDetallesActivos().size());
        // La linea sigue en la coleccion marcada como borrada: la baja debe llegar como UPDATE.
        assertEquals(2, lote.getDetalles().size());
        assertTrue(lote.getDetalles().get(0).isBorrado());
        assertEquals(new BigDecimal("40.00"), lote.getTotal());
    }

    @Test
    void removerUnDetalleAjenoAlLoteFalla() {
        Lote lote = lotePersistido();

        DomainException error = assertThrows(DomainException.class,
                () -> lote.removerDetalle("99999999-9999-9999-9999-999999999999"));

        assertEquals(404, error.getCodigo());
    }

    @Test
    void eliminarElLoteArrastraSusLineas() {
        Lote lote = lotePersistido();

        lote.eliminar();

        assertTrue(lote.isBorrado());
        assertTrue(lote.getDetallesActivos().isEmpty());
        assertTrue(lote.getDetalles().stream().allMatch(DetalleLote::isBorrado));
        assertEquals(new BigDecimal("0.00"), lote.getTotal());
    }

    @Test
    void laColeccionDeDetallesNoSePuedeMutarDesdeFuera() {
        Lote lote = lotePersistido();

        List<DetalleLote> detalles = lote.getDetalles();

        assertThrows(UnsupportedOperationException.class, () -> detalles.remove(0));
    }

    @Test
    void unLoteSinLineasVivasNoEsRegistrable() {
        Lote lote = lotePersistido();
        lote.removerDetalle(ID_DETALLE_CAFE);
        lote.removerDetalle(ID_DETALLE_AZUCAR);

        DomainException error = assertThrows(DomainException.class, lote::validarRegistrable);

        assertEquals(400, error.getCodigo());
    }

    @Test
    void actualizarDetalleRecalculaElCostoDelLote() {
        Lote lote = lotePersistido();

        lote.actualizarDetalle(ID_DETALLE_AZUCAR, null, new BigDecimal("6.00"), null, null);

        assertEquals(new BigDecimal("168.00"), lote.getTotal());
        assertFalse(lote.getDetallesActivos().get(1).isBorrado());
    }

    private Lote loteDeProduccion() {
        return Lote.registrarProduccion(
                LocalDateTime.of(2026, 9, 27, 8, 0),
                new ProduccionId("66666666-6666-6666-6666-666666666666"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"),
                PRODUCTO_CAFE,
                new BigDecimal("20"),
                new BigDecimal("7.25"),
                LocalDate.of(2026, 9, 29));
    }

    @Test
    void unLoteDeCompraExigeProveedor() {
        DomainException error = assertThrows(DomainException.class, () -> Lote.crear(
                LocalDateTime.of(2026, 9, 17, 10, 30),
                null,
                new UsuarioId("22222222-2222-2222-2222-222222222222")));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unLoteDeProduccionNaceSinProveedorYConUnaLineaAbiertaPorCompleto() {
        Lote lote = loteDeProduccion();

        assertTrue(lote.isEsProduccion());
        assertEquals(null, lote.getProveedorId());
        assertEquals("66666666-6666-6666-6666-666666666666", lote.getProduccionId().getValue());
        assertEquals(1, lote.getDetallesActivos().size());
        assertEquals(new BigDecimal("20.00"), lote.getDetallesActivos().get(0).getCantidadInicial());
        assertEquals(new BigDecimal("0.00"), lote.getDetallesActivos().get(0).getCantidadRestante());
        assertEquals(new BigDecimal("145.00"), lote.getTotal());
    }

    @Test
    void unLoteDeProduccionExigeLaProduccionQueLoOrigina() {
        DomainException error = assertThrows(DomainException.class, () -> Lote.registrarProduccion(
                LocalDateTime.of(2026, 9, 27, 8, 0), null,
                new UsuarioId("22222222-2222-2222-2222-222222222222"),
                PRODUCTO_CAFE, new BigDecimal("20"), new BigDecimal("7.25"), null));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unLoteDeProduccionNoAdmiteProveedor() {
        Lote lote = loteDeProduccion();

        DomainException error = assertThrows(DomainException.class, () -> lote.actualizar(
                null, new ProveedorId("11111111-1111-1111-1111-111111111111"), null));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void elCostoPorUnidadDeContenidoDeUnaCompraReparteElPrecioEntreElContenido() {
        Lote lote = lotePersistido();

        BigDecimal costoPorGramo = lote.costoPorUnidadDeContenido(new DetalleLoteId(ID_DETALLE_CAFE), new BigDecimal("1000"));

        assertEquals(0, new BigDecimal("0.012").compareTo(costoPorGramo));
    }
}
