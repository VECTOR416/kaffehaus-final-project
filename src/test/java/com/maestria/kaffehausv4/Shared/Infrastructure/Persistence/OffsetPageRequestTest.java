package com.maestria.kaffehausv4.Shared.Infrastructure.Persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Pruebas unitarias del Pageable por desplazamiento absoluto. */
class OffsetPageRequestTest {

    @Test
    void devuelveElDesplazamientoTalCualAunqueNoSeaMultiploDelLimite() {
        // Act
        OffsetPageRequest pagina = new OffsetPageRequest(7, 5);

        // Assert
        assertEquals(7, pagina.getOffset());
        assertEquals(5, pagina.getPageSize());
        assertEquals(1, pagina.getPageNumber());
        assertTrue(pagina.getSort().isUnsorted());
    }

    @Test
    void corrigeUnDesplazamientoNegativoYUnLimiteMenorQueUno() {
        // Act
        OffsetPageRequest pagina = new OffsetPageRequest(-3, 0);

        // Assert
        assertEquals(0, pagina.getOffset());
        assertEquals(1, pagina.getPageSize());
        assertFalse(pagina.hasPrevious());
    }

    @Test
    void nextAvanzaUnLimiteCompleto() {
        // Arrange
        OffsetPageRequest pagina = new OffsetPageRequest(7, 5);

        // Act
        Pageable siguiente = pagina.next();

        // Assert
        assertEquals(12, siguiente.getOffset());
        assertEquals(5, siguiente.getPageSize());
    }

    @Test
    void previousOrFirstRetrocedeUnLimiteSinBajarDeCero() {
        // Arrange
        OffsetPageRequest lejos = new OffsetPageRequest(12, 5);
        OffsetPageRequest cerca = new OffsetPageRequest(3, 5);
        OffsetPageRequest primera = new OffsetPageRequest(0, 5);

        // Act
        Pageable anteriorDeLejos = lejos.previousOrFirst();
        Pageable anteriorDeCerca = cerca.previousOrFirst();
        Pageable anteriorDePrimera = primera.previousOrFirst();

        // Assert
        assertEquals(7, anteriorDeLejos.getOffset());
        assertEquals(0, anteriorDeCerca.getOffset());
        assertEquals(0, anteriorDePrimera.getOffset());
    }

    @Test
    void firstYWithPageSeCalculanDesdeElLimite() {
        // Arrange
        OffsetPageRequest pagina = new OffsetPageRequest(12, 5);

        // Act
        Pageable primera = pagina.first();
        Pageable tercera = pagina.withPage(3);

        // Assert
        assertEquals(0, primera.getOffset());
        assertEquals(15, tercera.getOffset());
        assertTrue(pagina.hasPrevious());
    }
}
