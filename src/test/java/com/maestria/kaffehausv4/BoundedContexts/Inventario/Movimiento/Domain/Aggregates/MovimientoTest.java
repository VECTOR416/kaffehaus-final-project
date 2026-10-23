package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro: un movimiento es auditoria inmutable. */
class MovimientoTest {

    private Movimiento movimiento() {
        return Movimiento.restaurar(
                "13131313-1313-1313-1313-131313131313",
                new BigDecimal("18.0000"),
                LocalDateTime.of(2026, 9, 27, 9, 15),
                "Venta",
                new ExistenciaId("77777777-7777-7777-7777-777777777777"),
                new TipoMovimientoId("14141414-1414-1414-1414-141414141414"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"),
                null,
                false);
    }

    @Test
    void unMovimientoNoSePuedeModificar() {
        DomainException error = assertThrows(DomainException.class, movimiento()::actualizar);

        assertEquals(409, error.getCodigo());
    }

    @Test
    void unMovimientoNoSePuedeDarDeBaja() {
        Movimiento movimiento = movimiento();

        DomainException error = assertThrows(DomainException.class, movimiento::eliminar);

        assertEquals(409, error.getCodigo());
        assertFalse(movimiento.isBorrado());
    }

    @Test
    void laAperturaRegistraUnMovimientoDeCantidadCero() {
        Movimiento apertura = Movimiento.registrarApertura(
                LocalDateTime.of(2026, 9, 27, 9, 15),
                "Apertura de 'Bolsa de cafe'",
                new ExistenciaId("77777777-7777-7777-7777-777777777777"),
                new TipoMovimientoId("14141414-1414-1414-1414-141414141414"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"));

        assertEquals(0, BigDecimal.ZERO.compareTo(apertura.getCantidad()));
        assertEquals("Apertura de 'Bolsa de cafe'", apertura.getMotivo());
        assertFalse(apertura.isBorrado());
    }

    @Test
    void rechazaUnaCantidadNoPositiva() {
        DomainException error = assertThrows(DomainException.class, () -> Movimiento.crear(
                new BigDecimal("-1"),
                LocalDateTime.of(2026, 9, 27, 9, 15),
                null,
                new ExistenciaId("77777777-7777-7777-7777-777777777777"),
                new TipoMovimientoId("14141414-1414-1414-1414-141414141414"),
                new UsuarioId("22222222-2222-2222-2222-222222222222"),
                null));

        assertEquals(400, error.getCodigo());
    }
}
