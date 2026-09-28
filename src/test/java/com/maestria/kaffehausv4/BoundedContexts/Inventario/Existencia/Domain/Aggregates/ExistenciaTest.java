package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro del consumo de una existencia. */
class ExistenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 27, 9, 15);
    private static final EstadoExistenciaId ABIERTA = new EstadoExistenciaId("88888888-8888-8888-8888-888888888888");
    private static final EstadoExistenciaId AGOTADA = new EstadoExistenciaId("15151515-1515-1515-1515-151515151515");

    private Existencia bolsaDeCafe(String contenidoActual) {
        return Existencia.restaurar(
                "77777777-7777-7777-7777-777777777777",
                new BigDecimal("1000.0000"),
                new BigDecimal(contenidoActual),
                null,
                null,
                LocalDateTime.of(2026, 9, 20, 7, 0),
                new DetalleLoteId("44444444-4444-4444-4444-444444444444"),
                ABIERTA,
                new UnidadId("99999999-9999-9999-9999-999999999999"),
                null,
                false);
    }

    @Test
    void consumirDescuentaElContenido() {
        Existencia existencia = bolsaDeCafe("1000.0000");

        existencia.consumir(new BigDecimal("18"), AHORA, AGOTADA);

        assertEquals(new BigDecimal("982.0000"), existencia.getContenidoActual());
        assertNull(existencia.getFechaAcabado());
        assertEquals(ABIERTA.getValue(), existencia.getEstadoExistenciaId().getValue());
    }

    @Test
    void consumirTodoElContenidoLaDejaAgotada() {
        Existencia existencia = bolsaDeCafe("18.0000");

        existencia.consumir(new BigDecimal("18"), AHORA, AGOTADA);

        assertEquals(0, existencia.getContenidoActual().signum());
        assertEquals(AHORA, existencia.getFechaAcabado());
        assertEquals(AGOTADA.getValue(), existencia.getEstadoExistenciaId().getValue());
    }

    @Test
    void rechazaConsumirMasDeLoQueHay() {
        Existencia existencia = bolsaDeCafe("10.0000");

        DomainException error = assertThrows(DomainException.class,
                () -> existencia.consumir(new BigDecimal("18"), AHORA, AGOTADA));

        assertEquals(409, error.getCodigo());
        assertEquals(new BigDecimal("10.0000"), existencia.getContenidoActual());
    }

    @Test
    void rechazaUnConsumoNoPositivo() {
        Existencia existencia = bolsaDeCafe("10.0000");

        DomainException error = assertThrows(DomainException.class,
                () -> existencia.consumir(BigDecimal.ZERO, AHORA, AGOTADA));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void ajustarAUnConteoMenorDevuelveLaDiferenciaNegativa() {
        Existencia existencia = bolsaDeCafe("500.0000");

        BigDecimal diferencia = existencia.ajustarA(new BigDecimal("480"), AHORA, AGOTADA);

        assertEquals(new BigDecimal("-20.0000"), diferencia);
        assertEquals(new BigDecimal("480.0000"), existencia.getContenidoActual());
    }

    @Test
    void ajustarAUnConteoMayorDevuelveLaDiferenciaPositiva() {
        Existencia existencia = bolsaDeCafe("500.0000");

        assertEquals(new BigDecimal("15.0000"), existencia.ajustarA(new BigDecimal("515"), AHORA, AGOTADA));
    }

    @Test
    void unAjustaACeroLaDejaAgotada() {
        Existencia existencia = bolsaDeCafe("5.0000");

        existencia.ajustarA(BigDecimal.ZERO, AHORA, AGOTADA);

        assertTrue(existencia.isAgotada());
        assertEquals(AGOTADA.getValue(), existencia.getEstadoExistenciaId().getValue());
    }

    @Test
    void noSeAjustaUnaExistenciaAgotada() {
        Existencia existencia = bolsaDeCafe("0.0000");

        DomainException error = assertThrows(DomainException.class, () -> existencia.ajustarA(BigDecimal.TEN, AHORA, AGOTADA));

        assertEquals(409, error.getCodigo());
    }

    @Test
    void unConteoNoPuedeSuperarElContenidoInicial() {
        Existencia existencia = bolsaDeCafe("500.0000");

        DomainException error = assertThrows(DomainException.class, () -> existencia.ajustarA(new BigDecimal("1001"), AHORA, AGOTADA));

        assertEquals(409, error.getCodigo());
    }

    @Test
    void darDeBajaTodoDevuelveLoQueQuedaba() {
        Existencia existencia = bolsaDeCafe("320.0000");

        assertEquals(new BigDecimal("320.0000"), existencia.darDeBajaTodo(AHORA, AGOTADA));
        assertTrue(existencia.isAgotada());
    }

    @Test
    void noSeDaDeBajaLogicaUnaExistenciaConContenido() {
        DomainException error = assertThrows(DomainException.class, () -> bolsaDeCafe("1.0000").eliminar());

        assertEquals(409, error.getCodigo());
    }

    @Test
    void sinFechaDeVencimientoNuncaVence() {
        assertTrue(!bolsaDeCafe("1.0000").estaVencida(LocalDate.of(2099, 1, 1)));
    }
}
