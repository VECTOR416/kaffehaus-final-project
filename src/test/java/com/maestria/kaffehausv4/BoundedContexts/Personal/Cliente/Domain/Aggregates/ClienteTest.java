package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de Cliente y de su comodin 'Mostrador'. */
class ClienteTest {

    private static final String ID = "c1000000-0000-0000-0000-000000000009";
    private static final String PERSONA = "c1000000-0000-0000-0000-000000000010";

    @Test
    void crearNaceSinIdYLigadoASuPersona() {
        // Act
        Cliente cliente = Cliente.crear(PERSONA);

        // Assert
        assertNull(cliente.getId());
        assertEquals(PERSONA, cliente.getPersona().getValue());
        assertFalse(cliente.isBorrado());
        assertFalse(cliente.esMostrador());
    }

    @Test
    void unClienteCualquieraSeDaDeBajaLogica() {
        // Arrange
        Cliente cliente = Cliente.restaurar(ID, PERSONA, false);

        // Act
        cliente.eliminar();

        // Assert
        assertTrue(cliente.isBorrado());
    }

    @Test
    void elMostradorSeReconocePorSuIdFijo() {
        // Arrange
        Cliente mostrador = Cliente.restaurar(Cliente.ID_MOSTRADOR, Cliente.ID_MOSTRADOR, false);

        // Act
        boolean esMostrador = mostrador.esMostrador();

        // Assert
        assertTrue(esMostrador);
    }

    @Test
    void elMostradorNoSePuedeDarDeBaja() {
        // Arrange
        Cliente mostrador = Cliente.restaurar(Cliente.ID_MOSTRADOR, Cliente.ID_MOSTRADOR, false);

        // Act
        DomainException error = assertThrows(DomainException.class, mostrador::eliminar);

        // Assert
        assertEquals(409, error.getCodigo());
        assertFalse(mostrador.isBorrado());
    }

    @Test
    void actualizarCambiaLaPersonaYConNuloLaConserva() {
        // Arrange
        Cliente cliente = Cliente.restaurar(ID, PERSONA, false);

        // Act
        cliente.actualizar(null);
        String trasNulo = cliente.getPersona().getValue();
        cliente.actualizar("c1000000-0000-0000-0000-000000000011");

        // Assert
        assertEquals(PERSONA, trasNulo);
        assertEquals("c1000000-0000-0000-0000-000000000011", cliente.getPersona().getValue());
    }
}
