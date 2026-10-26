package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

/** Pruebas de dominio puro de Proveedor: una persona, con o sin entidad detras. */
class ProveedorTest {

    private static final String ID = "f0000000-0000-0000-0000-000000000001";
    private static final PersonaId PERSONA = new PersonaId("f0000000-0000-0000-0000-000000000002");
    private static final EntidadId ENTIDAD = new EntidadId("f0000000-0000-0000-0000-000000000003");

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Proveedor proveedor = Proveedor.crear(PERSONA, ENTIDAD);

        // Assert
        assertNull(proveedor.getId());
        assertSame(PERSONA, proveedor.getPersonaId());
        assertSame(ENTIDAD, proveedor.getEntidadId());
        assertFalse(proveedor.isBorrado());
    }

    @Test
    void unProveedorPuedeNoTenerEntidad() {
        // Act
        Proveedor proveedor = Proveedor.crear(PERSONA, null);

        // Assert
        assertNull(proveedor.getEntidadId());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Proveedor proveedor = Proveedor.restaurar(ID, PERSONA, ENTIDAD, null, null, true);

        // Assert
        assertEquals(ID, proveedor.getId().getValue());
        assertTrue(proveedor.isBorrado());
    }

    @Test
    void actualizarConservaLaPersonaSiNoLlegaPeroSiempreReemplazaLaEntidad() {
        // Arrange
        Proveedor proveedor = Proveedor.restaurar(ID, PERSONA, ENTIDAD, null, null, false);

        // Act
        proveedor.actualizar(null, null);

        // Assert: la entidad es opcional, asi que un nulo significa "sin entidad".
        assertSame(PERSONA, proveedor.getPersonaId());
        assertNull(proveedor.getEntidadId());
    }

    @Test
    void actualizarCambiaPersonaYEntidad() {
        // Arrange
        Proveedor proveedor = Proveedor.restaurar(ID, PERSONA, null, null, null, false);
        PersonaId otraPersona = new PersonaId("f0000000-0000-0000-0000-000000000004");

        // Act
        proveedor.actualizar(otraPersona, ENTIDAD);

        // Assert
        assertSame(otraPersona, proveedor.getPersonaId());
        assertSame(ENTIDAD, proveedor.getEntidadId());
    }

    @Test
    void eliminarEsUnaBajaLogica() {
        // Arrange
        Proveedor proveedor = Proveedor.restaurar(ID, PERSONA, ENTIDAD, null, null, false);

        // Act
        proveedor.eliminar();

        // Assert
        assertTrue(proveedor.isBorrado());
        assertSame(PERSONA, proveedor.getPersonaId());
    }
}
