package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de EstadoPedido: catalogo de estados de un pedido. */
class EstadoPedidoTest {

    private static final String ID = "c0000004-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        EstadoPedido estado = EstadoPedido.crear("Recibido");

        // Assert
        assertNull(estado.getId());
        assertEquals("Recibido", estado.getNombre());
        assertFalse(estado.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        EstadoPedido estado = EstadoPedido.restaurar(ID, "Recibido", true);

        // Assert
        assertEquals(ID, estado.getId().getValue());
        assertEquals("Recibido", estado.getNombre());
        assertTrue(estado.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        EstadoPedido estado = EstadoPedido.restaurar(ID, "Demorado", false);

        // Act
        estado.actualizar("En camino");

        // Assert
        assertEquals("En camino", estado.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        EstadoPedido estado = EstadoPedido.restaurar(ID, "Recibido", false);

        // Act
        estado.actualizar(null);

        // Assert
        assertEquals("Recibido", estado.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        EstadoPedido estado = EstadoPedido.restaurar(ID, "Demorado", false);

        // Act
        estado.eliminar();

        // Assert
        assertTrue(estado.isBorrado());
        assertEquals("Demorado", estado.getNombre());
    }

    @Test
    void unaFilaSemanticaNoSeRenombraNiSeDaDeBaja() {
        // Arrange (logica.md GENERAL-19)
        EstadoPedido semantica = EstadoPedido.restaurar(ID, "Recibido", false);

        // Act
        DomainException renombrar = assertThrows(DomainException.class, () -> semantica.actualizar("Otro nombre"));
        DomainException darDeBaja = assertThrows(DomainException.class, semantica::eliminar);
        semantica.actualizar("RECIBIDO");

        // Assert
        assertEquals(409, renombrar.getCodigo());
        assertEquals(409, darDeBaja.getCodigo());
        assertEquals("Recibido", semantica.getNombre());
        assertFalse(semantica.isBorrado());
        assertTrue(semantica.esSemantico());
        assertFalse(EstadoPedido.restaurar(ID, "Demorado", false).esSemantico());
    }
}
