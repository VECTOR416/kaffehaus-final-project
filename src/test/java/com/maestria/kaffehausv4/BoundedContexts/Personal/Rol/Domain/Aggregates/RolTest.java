package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de Rol: catalogo de roles. */
class RolTest {

    private static final String ID = "c0000005-0000-0000-0000-000000000001";

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Rol rol = Rol.crear("Administrador");

        // Assert
        assertNull(rol.getId());
        assertEquals("Administrador", rol.getNombre());
        assertFalse(rol.isBorrado());
    }

    @Test
    void restaurarConservaLosDatosPersistidos() {
        // Act
        Rol rol = Rol.restaurar(ID, "Administrador", true);

        // Assert
        assertEquals(ID, rol.getId().getValue());
        assertEquals("Administrador", rol.getNombre());
        assertTrue(rol.isBorrado());
    }

    @Test
    void actualizarCambiaElNombre() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Supervisor", false);

        // Act
        rol.actualizar("Operador");

        // Assert
        assertEquals("Operador", rol.getNombre());
    }

    @Test
    void actualizarConNuloConservaElNombre() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Administrador", false);

        // Act
        rol.actualizar(null);

        // Assert
        assertEquals("Administrador", rol.getNombre());
    }

    @Test
    void eliminarEsUnaBajaLogicaQueConservaLosDatos() {
        // Arrange
        Rol rol = Rol.restaurar(ID, "Supervisor", false);

        // Act
        rol.eliminar();

        // Assert
        assertTrue(rol.isBorrado());
        assertEquals("Supervisor", rol.getNombre());
    }

    @Test
    void unaFilaSemanticaNoSeRenombraNiSeDaDeBaja() {
        // Arrange (logica.md GENERAL-19)
        Rol semantica = Rol.restaurar(ID, "Administrador", false);

        // Act
        DomainException renombrar = assertThrows(DomainException.class, () -> semantica.actualizar("Otro nombre"));
        DomainException darDeBaja = assertThrows(DomainException.class, semantica::eliminar);
        semantica.actualizar("ADMINISTRADOR");

        // Assert
        assertEquals(409, renombrar.getCodigo());
        assertEquals(409, darDeBaja.getCodigo());
        assertEquals("Administrador", semantica.getNombre());
        assertFalse(semantica.isBorrado());
        assertTrue(semantica.esSemantico());
        assertFalse(Rol.restaurar(ID, "Supervisor", false).esSemantico());
    }
}
