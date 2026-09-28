package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

/** Pruebas de dominio puro de la resolucion de factores de conversion (inventario.md INV-09). */
class ConversionTest {

    private static final UnidadId BOLSA_5KG = new UnidadId("a0000000-0000-0000-0000-000000000001");
    private static final UnidadId KG = new UnidadId("a0000000-0000-0000-0000-000000000002");
    private static final UnidadId G = new UnidadId("a0000000-0000-0000-0000-000000000003");
    private static final UnidadId L = new UnidadId("a0000000-0000-0000-0000-000000000004");
    private static final UnidadId ML = new UnidadId("a0000000-0000-0000-0000-000000000005");

    private static Conversion conversion(String id, UnidadId origen, UnidadId destino, String factor) {
        return Conversion.restaurar(id, new BigDecimal(factor), true, origen, destino, false);
    }

    private static final List<Conversion> TABLA = List.of(
            conversion("c0000000-0000-0000-0000-000000000001", BOLSA_5KG, KG, "5"),
            conversion("c0000000-0000-0000-0000-000000000002", KG, G, "1000"),
            conversion("c0000000-0000-0000-0000-000000000003", L, ML, "1000"));

    @Test
    void laMismaUnidadTieneFactorUno() {
        assertEquals(0, BigDecimal.ONE.compareTo(Conversion.factorEntre(G, G, TABLA).orElseThrow()));
    }

    @Test
    void usaLaFilaDirecta() {
        assertEquals(0, new BigDecimal("1000").compareTo(Conversion.factorEntre(KG, G, TABLA).orElseThrow()));
    }

    @Test
    void usaLaFilaInversaDividiendo() {
        assertEquals(0, new BigDecimal("0.001").compareTo(Conversion.factorEntre(G, KG, TABLA).orElseThrow()));
    }

    @Test
    void encadenaConversionesHastaLaUnidadMinima() {
        assertEquals(0, new BigDecimal("5000").compareTo(Conversion.factorEntre(BOLSA_5KG, G, TABLA).orElseThrow()));
    }

    @Test
    void sinCaminoNoHayFactor() {
        assertTrue(Conversion.factorEntre(BOLSA_5KG, ML, TABLA).isEmpty());
    }

    @Test
    void ignoraLasConversionesDadasDeBaja() {
        Conversion borrada = Conversion.restaurar("c0000000-0000-0000-0000-000000000009", new BigDecimal("5"), true, BOLSA_5KG, KG, true);

        assertTrue(Conversion.factorEntre(BOLSA_5KG, KG, List.of(borrada)).isEmpty());
    }
}
