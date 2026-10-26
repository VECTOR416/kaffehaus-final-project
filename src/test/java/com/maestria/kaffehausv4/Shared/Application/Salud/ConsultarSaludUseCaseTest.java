package com.maestria.kaffehausv4.Shared.Application.Salud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarSaludUseCaseTest {

    @Mock private IVerificadorDeBaseDeDatos verificadorDeBaseDeDatos;
    @InjectMocks private ConsultarSaludUseCase consultarSaludUseCase;

    @Test
    void conLaBaseDisponibleElSistemaEstaUp() {
        // Arrange
        when(verificadorDeBaseDeDatos.estaDisponible()).thenReturn(true);

        // Act
        SaludResponse salud = consultarSaludUseCase.execute();

        // Assert
        assertEquals("UP", salud.getEstado());
        assertEquals("UP", salud.getBaseDeDatos());
    }

    @Test
    void conLaBaseCaidaElSistemaEstaDown() {
        // Arrange
        when(verificadorDeBaseDeDatos.estaDisponible()).thenReturn(false);

        // Act
        SaludResponse salud = consultarSaludUseCase.execute();

        // Assert
        assertEquals("DOWN", salud.getEstado());
        assertEquals("DOWN", salud.getBaseDeDatos());
    }
}
