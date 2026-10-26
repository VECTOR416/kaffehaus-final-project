package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro del agregado Produccion. */
class ProduccionTest {

    private static final RecetaId RECETA = new RecetaId("12121212-1212-1212-1212-121212121212");
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 9, 27, 6, 30);

    @Test
    void laDiferenciaSeDerivaDeLaCantidadRealYLaEsperada() {
        Produccion produccion = Produccion.crear(new BigDecimal("20"), new BigDecimal("18"), "Dos se quemaron", FECHA, RECETA);

        assertEquals(new BigDecimal("-2.00"), produccion.getDiferencia());
        assertTrue(produccion.produjoUnidades());
    }

    @Test
    void elFactorEscalaLaRecetaALaCantidadEsperada() {
        Produccion produccion = Produccion.crear(new BigDecimal("20"), new BigDecimal("20"), null, FECHA, RECETA);

        assertEquals(0, new BigDecimal("2").compareTo(produccion.factorSobre(new BigDecimal("10"))));
    }

    @Test
    void unaTandaPerdidaNoProduceUnidades() {
        Produccion produccion = Produccion.crear(new BigDecimal("20"), BigDecimal.ZERO, "Se corto la salsa", FECHA, RECETA);

        assertFalse(produccion.produjoUnidades());
        assertEquals(new BigDecimal("-20.00"), produccion.getDiferencia());
    }

    @Test
    void rechazaUnaCantidadEsperadaNoPositiva() {
        DomainException error = assertThrows(DomainException.class,
                () -> Produccion.crear(BigDecimal.ZERO, BigDecimal.ONE, null, FECHA, RECETA));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unaProduccionNoSeDaDeBaja() {
        Produccion produccion = Produccion.crear(new BigDecimal("20"), new BigDecimal("18"), null, FECHA, RECETA);

        DomainException error = assertThrows(DomainException.class, produccion::eliminar);

        assertEquals(409, error.getCodigo());
        assertFalse(produccion.isBorrado());
    }

    @Test
    void actualizarSoloTocaElMotivoYLaFecha() {
        Produccion produccion = Produccion.crear(new BigDecimal("20"), new BigDecimal("18"), null, FECHA, RECETA);

        produccion.actualizar("Horno desnivelado", null);

        assertEquals("Horno desnivelado", produccion.getMotivoDiferencia());
        assertEquals(FECHA, produccion.getFechaProduccion());
        assertEquals(new BigDecimal("18.00"), produccion.getCantidadReal());
    }
}
