package com.maestria.kaffehausv4.Shared.Presentation.Salud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.maestria.kaffehausv4.Shared.Application.Salud.ConsultarSaludUseCase;
import com.maestria.kaffehausv4.Shared.Application.Salud.SaludResponse;

@ExtendWith(MockitoExtension.class)
class SaludControllerV1Test {

    @Mock private ConsultarSaludUseCase consultarSaludUseCase;
    @InjectMocks private SaludControllerV1 controller;

    @Test
    void sistemaDisponibleResponde200() {
        // Arrange
        when(consultarSaludUseCase.execute()).thenReturn(SaludResponse.crear(true));

        // Act
        ResponseEntity<SaludResponse> respuesta = controller.consultar();

        // Assert
        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("UP", respuesta.getBody().getEstado());
    }

    @Test
    void sistemaNoDisponibleResponde503ConElMismoCuerpo() {
        // Arrange
        when(consultarSaludUseCase.execute()).thenReturn(SaludResponse.crear(false));

        // Act
        ResponseEntity<SaludResponse> respuesta = controller.consultar();

        // Assert
        assertEquals(503, respuesta.getStatusCode().value());
        assertEquals("DOWN", respuesta.getBody().getEstado());
        assertEquals("DOWN", respuesta.getBody().getBaseDeDatos());
    }
}
