package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de una linea de lote: cantidades, apertura y vencimiento. */
class DetalleLoteTest {

    private static final ProductoId CAFE = new ProductoId("aaaaaaaa-0000-0000-0000-000000000001");
    private static final LocalDate VENCE = LocalDate.of(2026, 12, 31);

    private static DetalleLote detalle(String cantidad, String precio) {
        return DetalleLote.crear(CAFE, new BigDecimal(cantidad), new BigDecimal(precio), true, VENCE);
    }

    @Test
    void crearNaceConLaCantidadRestanteIgualALaInicial() {
        // Act
        DetalleLote detalle = detalle("10", "12.345");

        // Assert
        assertNull(detalle.getId());
        assertSame(CAFE, detalle.getProductoId());
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadInicial());
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadRestante());
        assertEquals(new BigDecimal("12.35"), detalle.getPrecioUnitario());
        assertTrue(detalle.isFraccionable());
        assertEquals(VENCE, detalle.getFechaVencimiento());
        assertFalse(detalle.isBorrado());
    }

    @Test
    void crearTrataFraccionableNuloComoFalso() {
        // Act
        DetalleLote detalle = DetalleLote.crear(CAFE, BigDecimal.ONE, BigDecimal.ONE, null, null);

        // Assert
        assertFalse(detalle.isFraccionable());
        assertNull(detalle.getFechaVencimiento());
    }

    @Test
    void crearExigeUnProducto() {
        // Act
        DomainException sinProducto = assertThrows(DomainException.class,
                () -> DetalleLote.crear(null, BigDecimal.ONE, BigDecimal.ONE, false, null));
        DomainException sinId = assertThrows(DomainException.class,
                () -> DetalleLote.crear(new ProductoId(null), BigDecimal.ONE, BigDecimal.ONE, false, null));

        // Assert
        assertEquals(400, sinProducto.getCodigo());
        assertEquals(400, sinId.getCodigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3"})
    void crearRechazaUnaCantidadInicialQueNoEsPositiva(String cantidad) {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> detalle(cantidad, "5"));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearRechazaUnPrecioNegativoPeroAceptaCero() {
        // Act
        DetalleLote regalado = detalle("1", "0");
        DomainException error = assertThrows(DomainException.class, () -> detalle("1", "-0.01"));

        // Assert
        assertEquals(new BigDecimal("0.00"), regalado.getPrecioUnitario());
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearRechazaCantidadOPrecioNulos() {
        // Act
        DomainException sinCantidad = assertThrows(DomainException.class,
                () -> DetalleLote.crear(CAFE, null, BigDecimal.ONE, false, null));
        DomainException sinPrecio = assertThrows(DomainException.class,
                () -> DetalleLote.crear(CAFE, BigDecimal.ONE, null, false, null));

        // Assert
        assertEquals(400, sinCantidad.getCodigo());
        assertEquals(400, sinPrecio.getCodigo());
    }

    @Test
    void costoTotalEsCantidadInicialPorPrecio() {
        // Arrange
        DetalleLote detalle = detalle("3", "12.35");

        // Act
        BigDecimal costo = detalle.costoTotal();

        // Assert
        assertEquals(new BigDecimal("37.05"), costo);
    }

    @Test
    void ajustarCantidadRestanteAceptaDesdeCeroHastaLaInicial() {
        // Arrange
        DetalleLote detalle = detalle("10", "5");

        // Act
        detalle.ajustarCantidadRestante(BigDecimal.ZERO);
        BigDecimal enCero = detalle.getCantidadRestante();
        detalle.ajustarCantidadRestante(new BigDecimal("10"));

        // Assert
        assertEquals(new BigDecimal("0.00"), enCero);
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadRestante());
    }

    @Test
    void ajustarCantidadRestanteRechazaNegativosNulosYMasDeLoComprado() {
        // Arrange
        DetalleLote detalle = detalle("10", "5");

        // Act
        DomainException porNegativa = assertThrows(DomainException.class, () -> detalle.ajustarCantidadRestante(new BigDecimal("-1")));
        DomainException porNula = assertThrows(DomainException.class, () -> detalle.ajustarCantidadRestante(null));
        DomainException porExceso = assertThrows(DomainException.class, () -> detalle.ajustarCantidadRestante(new BigDecimal("10.01")));

        // Assert
        assertEquals(400, porNegativa.getCodigo());
        assertEquals(400, porNula.getCodigo());
        assertEquals(400, porExceso.getCodigo());
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadRestante());
    }

    @Test
    void abrirUnidadDescuentaUnaUnidadCerrada() {
        // Arrange
        DetalleLote detalle = detalle("2", "5");

        // Act
        detalle.abrirUnidad();

        // Assert
        assertEquals(new BigDecimal("1.00"), detalle.getCantidadRestante());
        assertTrue(detalle.tieneUnidadesCerradas());
    }

    @Test
    void abrirUnidadFallaCuandoNoQuedaUnaUnidadEntera() {
        // Arrange
        DetalleLote detalle = detalle("1", "5");
        detalle.ajustarCantidadRestante(new BigDecimal("0.99"));

        // Act
        DomainException error = assertThrows(DomainException.class, detalle::abrirUnidad);

        // Assert
        assertEquals(409, error.getCodigo());
        assertFalse(detalle.tieneUnidadesCerradas());
        assertEquals(new BigDecimal("0.99"), detalle.getCantidadRestante());
    }

    @Test
    void estaVencidoSoloSiLaCaducidadEsAnteriorALaFecha() {
        // Arrange
        DetalleLote detalle = detalle("1", "5");

        // Act y Assert
        assertFalse(detalle.estaVencido(VENCE.minusDays(1)));
        assertFalse(detalle.estaVencido(VENCE));
        assertTrue(detalle.estaVencido(VENCE.plusDays(1)));
    }

    @Test
    void sinCaducidadNuncaVence() {
        // Arrange
        DetalleLote detalle = DetalleLote.crear(CAFE, BigDecimal.ONE, BigDecimal.ONE, false, null);

        // Act
        boolean vencido = detalle.estaVencido(LocalDate.of(2999, 1, 1));

        // Assert
        assertFalse(vencido);
    }

    @Test
    void actualizarDatosDeCompraCambiaSoloLoInformado() {
        // Arrange
        DetalleLote detalle = detalle("10", "5");

        // Act
        detalle.actualizarDatosDeCompra(new BigDecimal("12"), null, false, null);

        // Assert
        assertEquals(new BigDecimal("12.00"), detalle.getCantidadInicial());
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadRestante());
        assertEquals(new BigDecimal("5.00"), detalle.getPrecioUnitario());
        assertFalse(detalle.isFraccionable());
        assertEquals(VENCE, detalle.getFechaVencimiento());
    }

    @Test
    void actualizarDatosDeCompraNoDejaLaInicialPorDebajoDeLaRestante() {
        // Arrange
        DetalleLote detalle = detalle("10", "5");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> detalle.actualizarDatosDeCompra(new BigDecimal("9.99"), null, null, null));

        // Assert
        assertEquals(400, error.getCodigo());
        assertEquals(new BigDecimal("10.00"), detalle.getCantidadInicial());
    }

    @Test
    void actualizarDatosDeCompraValidaElNuevoPrecio() {
        // Arrange
        DetalleLote detalle = detalle("10", "5");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> detalle.actualizarDatosDeCompra(null, new BigDecimal("-1"), null, null));

        // Assert
        assertEquals(400, error.getCodigo());
        assertEquals(new BigDecimal("5.00"), detalle.getPrecioUnitario());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        DetalleLote detalle = DetalleLote.restaurar("aaaaaaaa-0000-0000-0000-000000000002", CAFE,
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, false, null, false);

        // Act
        detalle.eliminar();

        // Assert
        assertTrue(detalle.isBorrado());
        assertEquals(BigDecimal.ONE, detalle.getCantidadRestante());
    }
}
