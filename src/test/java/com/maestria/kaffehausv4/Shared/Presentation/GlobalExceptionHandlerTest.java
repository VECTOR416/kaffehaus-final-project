package com.maestria.kaffehausv4.Shared.Presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;
import com.maestria.kaffehausv4.Shared.Presentation.GlobalExceptionHandler.ErrorResponse;

/** Pruebas unitarias de la traduccion de excepciones a respuestas HTTP. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unaExcepcionDeDominioConservaSuCodigoTipoYMensaje() {
        // Arrange
        DomainException error = DomainException.throwException(409, "Stock insuficiente");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleDomainException(error);

        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().codigo());
        assertEquals("Domain Exception", respuesta.getBody().tipo());
        assertEquals("Stock insuficiente", respuesta.getBody().mensaje());
        assertNotNull(respuesta.getBody().timestamp());
    }

    @Test
    void unaExcepcionDeSistemaConservaSuCodigoYTipo() {
        // Arrange
        SystemException error = SystemException.throwException(500, "Fallo interno");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleSystemException(error);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("System Exception", respuesta.getBody().tipo());
        assertEquals("Fallo interno", respuesta.getBody().mensaje());
    }

    @Test
    void unaExcepcionDeAutorizacionSeAtiendeComoBaseException() {
        // Arrange
        AutorizacionException error = AutorizacionException.throwException(403, "Sin acceso");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleBaseException(error);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Authorization Exception", respuesta.getBody().tipo());
    }

    @Test
    void unCodigoQueNoEsHttpSeRespondeComoErrorInternoPeroConservaElCodigoEnElCuerpo() {
        // Arrange
        DomainException error = DomainException.throwException(999, "Codigo raro");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleDomainException(error);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals(999, respuesta.getBody().codigo());
    }

    @Test
    void unBloqueoDeConcurrenciaEsUnConflictoReintentable() {
        // Arrange
        PessimisticLockingFailureException error = new PessimisticLockingFailureException("deadlock");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleConcurrencia(error);

        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Domain Exception", respuesta.getBody().tipo());
    }

    @Test
    void chocarConLaClaveDeIdempotenciaEsUnConflicto() {
        // Arrange
        DataIntegrityViolationException error = new DataIntegrityViolationException("insert",
                new RuntimeException("duplicate key value violates unique constraint \"uq_venta_clave_idempotencia\""));

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleIntegridad(error);

        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().codigo());
    }

    @Test
    void abrirDosCajasALaVezEsUnConflicto() {
        // Arrange
        DataIntegrityViolationException error = new DataIntegrityViolationException("insert",
                new RuntimeException("violates unique constraint \"uq_sesion_caja_abierta\""));

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleIntegridad(error);

        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Ya hay una caja abierta; cierrala antes de abrir otra", respuesta.getBody().mensaje());
    }

    @Test
    void otraViolacionDeIntegridadSigueSiendoUnErrorInterno() {
        // Arrange
        DataIntegrityViolationException error = new DataIntegrityViolationException("insert",
                new RuntimeException("violates foreign key constraint \"fk_cualquiera\""));

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleIntegridad(error);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("Runtime Exception", respuesta.getBody().tipo());
    }

    @Test
    void unaExcepcionInesperadaSinMensajeUsaUnTextoGenerico() {
        // Arrange
        RuntimeException error = new RuntimeException();

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleRuntimeException(error);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("Ocurrio un error inesperado", respuesta.getBody().mensaje());
    }

    @Test
    void unaExcepcionInesperadaConMensajeLoExpone() {
        // Arrange
        RuntimeException error = new IllegalStateException("estado imposible");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleRuntimeException(error);

        // Assert
        assertEquals("estado imposible", respuesta.getBody().mensaje());
    }
}
