package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de un ingreso o egreso menor de caja. */
class MovimientoCajaTest {

    private static final UsuarioId USUARIO = new UsuarioId("22222222-2222-2222-2222-222222222222");

    @Test
    void crearNormalizaElTipoYRedondeaElMontoADosDecimales() {
        // Act
        MovimientoCaja movimiento = MovimientoCaja.crear(USUARIO, " ingreso ", new BigDecimal("10.005"), "Cambio inicial");

        // Assert
        assertNull(movimiento.getId());
        assertEquals(MovimientoCaja.INGRESO, movimiento.getTipo());
        assertEquals(new BigDecimal("10.01"), movimiento.getMonto());
        assertEquals("Cambio inicial", movimiento.getMotivo());
        assertSame(USUARIO, movimiento.getUsuarioId());
        assertNull(movimiento.getFecha());
        assertFalse(movimiento.isBorrado());
    }

    @Test
    void esIngresoDistingueElIngresoDelEgreso() {
        // Arrange
        MovimientoCaja ingreso = MovimientoCaja.crear(USUARIO, "INGRESO", new BigDecimal("5"), "Cambio");
        MovimientoCaja egreso = MovimientoCaja.crear(USUARIO, "egreso", new BigDecimal("5"), "Hielo");

        // Act y Assert
        assertTrue(ingreso.esIngreso());
        assertFalse(egreso.esIngreso());
        assertEquals(MovimientoCaja.EGRESO, egreso.getTipo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"RETIRO", "ingresos", "  "})
    void crearRechazaUnTipoQueNoEsIngresoNiEgreso(String tipo) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> MovimientoCaja.crear(USUARIO, tipo, new BigDecimal("5"), "Motivo"));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void crearRechazaUnMontoQueNoEsPositivo(String monto) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> MovimientoCaja.crear(USUARIO, "INGRESO", new BigDecimal(monto), "Motivo"));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearRechazaUnMontoNulo() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> MovimientoCaja.crear(USUARIO, "INGRESO", null, "Motivo"));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void crearExigeUnMotivo(String motivo) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> MovimientoCaja.crear(USUARIO, "EGRESO", new BigDecimal("5"), motivo));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void crearAceptaUnMotivoEnElLimiteYRechazaElQueLoSupera() {
        // Arrange
        String enElLimite = "a".repeat(MovimientoCaja.LONGITUD_MAXIMA_MOTIVO);
        String excedido = enElLimite + "a";

        // Act
        MovimientoCaja valido = MovimientoCaja.crear(USUARIO, "EGRESO", new BigDecimal("5"), enElLimite);
        DomainException error = assertThrows(DomainException.class,
                () -> MovimientoCaja.crear(USUARIO, "EGRESO", new BigDecimal("5"), excedido));

        // Assert
        assertEquals(enElLimite, valido.getMotivo());
        assertEquals(400, error.getCodigo());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Arrange
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 27, 9, 15);

        // Act
        MovimientoCaja movimiento = MovimientoCaja.restaurar(
                "33333333-3333-3333-3333-333333333333", USUARIO, MovimientoCaja.EGRESO, new BigDecimal("7.50"), "Hielo", fecha, null, true);

        // Assert
        assertEquals("33333333-3333-3333-3333-333333333333", movimiento.getId().getValue());
        assertEquals(new BigDecimal("7.50"), movimiento.getMonto());
        assertEquals(fecha, movimiento.getFecha());
        assertTrue(movimiento.isBorrado());
    }
}
