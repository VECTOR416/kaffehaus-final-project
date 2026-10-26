package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de un pago de venta (CAJ-10..CAJ-13). */
class PagoVentaTest {

    private static final TipoPagoId TIPO = new TipoPagoId("44444444-4444-4444-4444-444444444444");

    @Test
    void sinMontoRecibidoSeEntiendePagoExactoYSinCambio() {
        // Act
        PagoVenta pago = PagoVenta.crear(TIPO, true, new BigDecimal("25"), null, null);

        // Assert
        assertNull(pago.getId());
        assertEquals(new BigDecimal("25.00"), pago.getMonto());
        assertEquals(new BigDecimal("25.00"), pago.getMontoRecibido());
        assertEquals(new BigDecimal("0.00"), pago.getCambio());
        assertSame(TIPO, pago.getTipoPagoId());
        assertFalse(pago.isBorrado());
    }

    @Test
    void elEfectivoAdmiteRecibirMasYCalculaElCambio() {
        // Act
        PagoVenta pago = PagoVenta.crear(TIPO, true, new BigDecimal("37.50"), new BigDecimal("50"), null);

        // Assert
        assertEquals(new BigDecimal("50.00"), pago.getMontoRecibido());
        assertEquals(new BigDecimal("12.50"), pago.getCambio());
    }

    @Test
    void elEfectivoNoPuedeRecibirMenosQueElMonto() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> PagoVenta.crear(TIPO, true, new BigDecimal("37.50"), new BigDecimal("37.49"), null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void unMedioQueNoEsEfectivoCobraExactoYGuardaSuReferencia() {
        // Act
        PagoVenta pago = PagoVenta.crear(TIPO, false, new BigDecimal("18.40"), new BigDecimal("18.4"), "VOUCHER-991");

        // Assert
        assertEquals(new BigDecimal("18.40"), pago.getMonto());
        assertEquals(new BigDecimal("0.00"), pago.getCambio());
        assertEquals("VOUCHER-991", pago.getReferencia());
    }

    @ParameterizedTest
    @ValueSource(strings = {"20", "18.39"})
    void unMedioQueNoEsEfectivoNoAdmiteUnRecibidoDistintoDelMonto(String recibido) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> PagoVenta.crear(TIPO, false, new BigDecimal("18.40"), new BigDecimal(recibido), null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearExigeElTipoDePago() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> PagoVenta.crear(null, true, new BigDecimal("10"), null, null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    void crearRechazaUnMontoQueNoEsPositivo(String monto) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> PagoVenta.crear(TIPO, true, new BigDecimal(monto), null, null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearRechazaUnMontoNulo() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> PagoVenta.crear(TIPO, true, null, null, null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void losImportesSeRedondeanADosDecimales() {
        // Act
        PagoVenta pago = PagoVenta.crear(TIPO, true, new BigDecimal("10.005"), new BigDecimal("20.004"), null);

        // Assert
        assertEquals(new BigDecimal("10.01"), pago.getMonto());
        assertEquals(new BigDecimal("20.00"), pago.getMontoRecibido());
        assertEquals(new BigDecimal("9.99"), pago.getCambio());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        PagoVenta pago = PagoVenta.restaurar("55555555-5555-5555-5555-555555555555", TIPO,
                new BigDecimal("10.00"), new BigDecimal("20.00"), new BigDecimal("10.00"), "REF", true);

        // Assert
        assertEquals("55555555-5555-5555-5555-555555555555", pago.getId().getValue());
        assertEquals(new BigDecimal("10.00"), pago.getCambio());
        assertEquals("REF", pago.getReferencia());
        assertTrue(pago.isBorrado());
    }
}
