package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro del agregado SesionCaja (arquitectura.md 4.11). */
class SesionCajaTest {

    private static final UsuarioId TITULAR = new UsuarioId("11111111-1111-1111-1111-111111111111");
    private static final UsuarioId OTRO = new UsuarioId("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 9, 28, 8, 0);

    private SesionCaja abierta() {
        return SesionCaja.abrir(TITULAR, new BigDecimal("100"), APERTURA);
    }

    @Test
    void naceAbiertaConElFondoDeAperturaYSinMovimientos() {
        SesionCaja sesion = abierta();

        assertTrue(sesion.estaAbierta());
        assertEquals(SesionCaja.ABIERTA, sesion.getEstado());
        assertEquals(new BigDecimal("100.00"), sesion.getMontoApertura());
        assertTrue(sesion.getMovimientos().isEmpty());
    }

    @Test
    void rechazaUnFondoDeAperturaNegativo() {
        DomainException error = assertThrows(DomainException.class,
                () -> SesionCaja.abrir(TITULAR, new BigDecimal("-1"), APERTURA));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void elCierreCalculaElEsperadoYLaDiferencia() {
        SesionCaja sesion = abierta();
        sesion.registrarMovimiento(TITULAR, "ingreso", new BigDecimal("50"), "Cambio del dueno", BigDecimal.ZERO, false);
        sesion.registrarMovimiento(OTRO, "EGRESO", new BigDecimal("15.50"), "Hielo", BigDecimal.ZERO, true);

        sesion.cerrar(new BigDecimal("330"), new BigDecimal("200"), "Faltan 4.50", APERTURA.plusHours(8));

        // 100 + 200 + 50 - 15.50 = 334.50; declarado 330.00.
        assertEquals(new BigDecimal("334.50"), sesion.getMontoCierreEsperado());
        assertEquals(new BigDecimal("330.00"), sesion.getMontoCierreDeclarado());
        assertEquals(new BigDecimal("-4.50"), sesion.getDiferencia());
        assertEquals("Faltan 4.50", sesion.getMotivoDiferencia());
        assertEquals(SesionCaja.CERRADA, sesion.getEstado());
        assertEquals(APERTURA.plusHours(8), sesion.getFechaCierre());
        assertEquals(new BigDecimal("50.00"), sesion.getTotalIngresos());
        assertEquals(new BigDecimal("15.50"), sesion.getTotalEgresos());
    }

    @Test
    void unaCajaCerradaNoAdmiteMovimientosNiOtroCierre() {
        SesionCaja sesion = abierta();
        sesion.cerrar(new BigDecimal("100"), BigDecimal.ZERO, null, APERTURA.plusHours(1));

        DomainException movimiento = assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "INGRESO", BigDecimal.ONE, "x", BigDecimal.ZERO, false));
        DomainException cierre = assertThrows(DomainException.class,
                () -> sesion.cerrar(BigDecimal.ONE, BigDecimal.ZERO, null, APERTURA.plusHours(2)));

        assertEquals(409, movimiento.getCodigo());
        assertEquals(409, cierre.getCodigo());
        assertFalse(sesion.estaAbierta());
    }

    @Test
    void unEgresoNoPuedeDejarLaCajaEnNegativo() {
        SesionCaja sesion = abierta();

        // 100 de fondo + 20 de ventas en efectivo = 120 disponibles.
        DomainException error = assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "EGRESO", new BigDecimal("120.01"), "Compra", new BigDecimal("20"), false));
        sesion.registrarMovimiento(TITULAR, "EGRESO", new BigDecimal("120"), "Compra", new BigDecimal("20"), false);

        assertEquals(409, error.getCodigo());
        assertEquals(new BigDecimal("120.00"), sesion.getTotalEgresos());
        assertEquals(new BigDecimal("0.00"), sesion.efectivoEsperado(new BigDecimal("20")));
    }

    @Test
    void validaTipoMontoYMotivoDeUnMovimiento() {
        SesionCaja sesion = abierta();

        assertEquals(400, assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "PRESTAMO", BigDecimal.ONE, "x", BigDecimal.ZERO, false)).getCodigo());
        assertEquals(400, assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "EGRESO", BigDecimal.ZERO, "x", BigDecimal.ZERO, false)).getCodigo());
        assertEquals(400, assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "EGRESO", BigDecimal.ONE, " ", BigDecimal.ZERO, false)).getCodigo());
        assertEquals(400, assertThrows(DomainException.class,
                () -> sesion.registrarMovimiento(TITULAR, "EGRESO", BigDecimal.ONE, "x".repeat(256), BigDecimal.ZERO, false)).getCodigo());
        assertTrue(sesion.getMovimientos().isEmpty());
    }

    @Test
    void soloElTitularOUnAdministradorRegistranUnEgreso() {
        // logica.md SESIONCAJA-15
        SesionCaja sesion = abierta();

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> sesion.registrarMovimiento(OTRO, "EGRESO", BigDecimal.ONE, "Taxi", BigDecimal.ZERO, false));
        sesion.registrarMovimiento(TITULAR, "EGRESO", BigDecimal.ONE, "Hielo", BigDecimal.ZERO, false);
        sesion.registrarMovimiento(OTRO, "EGRESO", BigDecimal.ONE, "Gas", BigDecimal.ZERO, true);

        assertEquals(403, error.getCodigo());
        assertEquals(new BigDecimal("2.00"), sesion.getTotalEgresos());
    }

    @Test
    void cualquieraQueAtiendaRegistraUnIngreso() {
        SesionCaja sesion = abierta();

        sesion.registrarMovimiento(OTRO, "INGRESO", new BigDecimal("10"), "Cambio", BigDecimal.ZERO, false);

        assertEquals(new BigDecimal("10.00"), sesion.getTotalIngresos());
    }

    @Test
    void unaCajaAbiertaUnDiaAnteriorLoIndica() {
        // logica.md SESIONCAJA-17: la apertura fue el 28/09 a las 08:00.
        SesionCaja sesion = abierta();

        assertFalse(sesion.abiertaDesdeDiaAnterior(APERTURA.toLocalDate()));
        assertTrue(sesion.abiertaDesdeDiaAnterior(APERTURA.toLocalDate().plusDays(1)));
        sesion.cerrar(new BigDecimal("100"), BigDecimal.ZERO, null, APERTURA.plusHours(20));
        assertFalse(sesion.abiertaDesdeDiaAnterior(APERTURA.toLocalDate().plusDays(1)));
    }

    @Test
    void soloElTitularOUnAdministradorPuedenCerrar() {
        SesionCaja sesion = abierta();

        AutorizacionException error = assertThrows(AutorizacionException.class, () -> sesion.exigirQuePuedaCerrar(OTRO, false));

        assertEquals(403, error.getCodigo());
        sesion.exigirQuePuedaCerrar(TITULAR, false);
        sesion.exigirQuePuedaCerrar(OTRO, true);
    }

    @Test
    void unaSesionNoSeDaDeBaja() {
        DomainException error = assertThrows(DomainException.class, () -> abierta().eliminar());

        assertEquals(409, error.getCodigo());
    }
}
