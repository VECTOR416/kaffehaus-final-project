package com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Pruebas unitarias de los objetos de paginacion. */
class PaginationDTOTest {

    @Test
    void sinParametrosUsaLosValoresPorDefecto() {
        // Act
        PaginationDTO paginacion = new PaginationDTO(null, null, null);

        // Assert
        assertEquals(0, paginacion.getSkip());
        assertEquals(10, paginacion.getLimit());
        assertEquals("", paginacion.getSearch());
    }

    @Test
    void conservaLosParametrosInformados() {
        // Act
        PaginationDTO paginacion = new PaginationDTO(20, 5, "cafe");

        // Assert
        assertEquals(20, paginacion.getSkip());
        assertEquals(5, paginacion.getLimit());
        assertEquals("cafe", paginacion.getSearch());
    }

    @Test
    void laRespuestaPaginadaExponeLoQueRecibe() {
        // Arrange
        List<String> datos = List.of("a", "b");

        // Act
        PaginatedResponse<List<String>> respuesta = new PaginatedResponse<>(datos, 10, 2, 13, true);
        PaginatedResponse<List<String>> ultima = new PaginatedResponse<>(datos, 12, 2, 13, false);

        // Assert
        assertEquals(datos, respuesta.getData());
        assertEquals(10, respuesta.getSkip());
        assertEquals(2, respuesta.getLimit());
        assertEquals(13, respuesta.getTotal());
        assertTrue(respuesta.isHasMore());
        assertFalse(ultima.isHasMore());
    }
}
