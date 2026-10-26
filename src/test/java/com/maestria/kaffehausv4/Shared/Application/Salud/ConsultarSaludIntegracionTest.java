package com.maestria.kaffehausv4.Shared.Application.Salud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/** Health check contra la base real del perfil de pruebas (H2), con el verificador JDBC. */
class ConsultarSaludIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private ConsultarSaludUseCase consultarSaludUseCase;

    @Test
    void conLaBaseArribaElSistemaEstaUp() {
        // Act
        SaludResponse salud = consultarSaludUseCase.execute();

        // Assert
        assertEquals("UP", salud.getEstado());
        assertEquals("UP", salud.getBaseDeDatos());
    }
}
