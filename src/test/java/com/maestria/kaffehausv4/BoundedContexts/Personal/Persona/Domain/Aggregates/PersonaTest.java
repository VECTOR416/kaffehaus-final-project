package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de dominio puro de Persona. */
class PersonaTest {

    private static final String ID = "a1000000-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Persona persona = Persona.crear("Ana", "Rojas", "70000000", "1234567");

        // Assert
        assertNull(persona.getId());
        assertEquals("Ana", persona.getNombres());
        assertEquals("Rojas", persona.getApellidos());
        assertEquals("70000000", persona.getTelefono());
        assertEquals("1234567", persona.getCarnet());
        assertFalse(persona.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Persona persona = Persona.restaurar(ID, "Ana", "Rojas", null, "1234567", true);

        // Assert
        assertEquals(ID, persona.getId().getValue());
        assertNull(persona.getTelefono());
        assertTrue(persona.isBorrado());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        Persona persona = Persona.restaurar(ID, "Ana", "Rojas", "70000000", "1234567", false);

        // Act
        persona.actualizar("Ana Maria", null, "71111111", null);

        // Assert
        assertEquals("Ana Maria", persona.getNombres());
        assertEquals("Rojas", persona.getApellidos());
        assertEquals("71111111", persona.getTelefono());
        assertEquals("1234567", persona.getCarnet());
    }

    @Test
    void eliminarPersonaEsUnaBajaLogica() {
        // Arrange
        Persona persona = Persona.restaurar(ID, "Ana", "Rojas", null, "1234567", false);

        // Act
        persona.eliminarPersona();

        // Assert
        assertTrue(persona.isBorrado());
        assertEquals("Ana", persona.getNombres());
    }
}
