package com.maestria.kaffehausv4.Shared.Presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;
import com.maestria.kaffehausv4.Shared.Presentation.GlobalExceptionHandler.ErrorResponse;

import tools.jackson.databind.ObjectMapper;

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
    void unErrorEstructuradoLlevaSuCodigoDeNegocioYSuDetalle() {
        // Arrange: el detalle admite valores nulos y conserva el orden de sus claves (arquitectura.md 4.16).
        Map<String, Object> bolsa = new LinkedHashMap<>();
        bolsa.put("idDetalleLote", "d1");
        bolsa.put("fechaVencimiento", null);
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("faltantes", List.of(bolsa));
        DomainException error = DomainException.throwException(409, "APERTURA_REQUERIDA", "Abre una presentacion", detalle);

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleDomainException(error);

        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("APERTURA_REQUERIDA", respuesta.getBody().error());
        assertEquals(detalle, respuesta.getBody().detalle());
        assertThrows(UnsupportedOperationException.class, () -> error.getDetalle().put("otro", 1));
    }

    @Test
    void unErrorSinCodigoDeNegocioNoEscribeErrorNiDetalleEnElJson() {
        // Arrange
        ErrorResponse cuerpo = handler.handleDomainException(DomainException.throwException(404, "No existe")).getBody();

        // Act
        String json = new ObjectMapper().writeValueAsString(cuerpo);

        // Assert: el contrato de los errores existentes no cambia.
        assertFalse(json.contains("\"error\""));
        assertFalse(json.contains("\"detalle\""));
        assertTrue(json.contains("\"mensaje\":\"No existe\""));
    }

    @Test
    void unErrorEstructuradoEscribeErrorYDetalleEnElJson() {
        // Arrange
        ErrorResponse cuerpo = handler.handleDomainException(DomainException.throwException(
                409, "STOCK_INSUFICIENTE", "Falta stock", Map.of("faltantes", List.of()))).getBody();

        // Act
        String json = new ObjectMapper().writeValueAsString(cuerpo);

        // Assert
        assertTrue(json.contains("\"error\":\"STOCK_INSUFICIENTE\""));
        assertTrue(json.contains("\"detalle\":{\"faltantes\":[]}"));
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
