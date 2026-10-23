package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de TipoMovimiento: catalogo que clasifica los movimientos de inventario. */
class TipoMovimientoTest {

    private static final String ID = "c0000003-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        TipoMovimiento tipo = TipoMovimiento.crear("Receta");

        // Assert
        assertNull(tipo.getId());
        assertEquals("Receta", tipo.getNombre());
        assertFalse(tipo.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        TipoMovimiento tipo = TipoMovimiento.restaurar(ID, "Receta", true);

        // Assert
        assertEquals(ID, tipo.getId().getValue());
        assertEquals("Receta", tipo.getNombre());
        assertTrue(tipo.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        TipoMovimiento tipo = TipoMovimiento.restaurar(ID, "Receta", false);

        // Act
        tipo.actualizar("Ajuste");

        // Assert
        assertEquals("Ajuste", tipo.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        TipoMovimiento tipo = TipoMovimiento.restaurar(ID, "Receta", false);

        // Act
        tipo.actualizar(null);

        // Assert
        assertEquals("Receta", tipo.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        TipoMovimiento tipo = TipoMovimiento.restaurar(ID, "Receta", false);

        // Act
        tipo.eliminar();

        // Assert
        assertTrue(tipo.isBorrado());
        assertEquals("Receta", tipo.getNombre());
    }

    @Test
    void elCatalogoQueReconoceElSistemaSonCuatroNombres() {
        // Assert
        assertEquals("Produccion", TipoMovimiento.PRODUCCION);
        assertEquals("Ajuste", TipoMovimiento.AJUSTE);
        assertEquals("Merma", TipoMovimiento.MERMA);
        assertEquals("Apertura", TipoMovimiento.APERTURA);
    }

    @Test
    void losSieteTiposDeMovimiento22NoSeRenombranNiSeDanDeBaja() {
        // logica.md GENERAL-19: el antiguo 'Receta' ya no tiene semantica.
        for (String nombre : TipoMovimiento.NOMBRES_SEMANTICOS) {
            TipoMovimiento tipo = TipoMovimiento.restaurar(ID, nombre, false);

            assertEquals(409, assertThrows(DomainException.class, () -> tipo.actualizar("Otro")).getCodigo());
            assertEquals(409, assertThrows(DomainException.class, tipo::eliminar).getCodigo());
        }
        assertEquals(7, TipoMovimiento.NOMBRES_SEMANTICOS.size());
        assertFalse(TipoMovimiento.restaurar(ID, "Receta", false).esSemantico());
    }
}
