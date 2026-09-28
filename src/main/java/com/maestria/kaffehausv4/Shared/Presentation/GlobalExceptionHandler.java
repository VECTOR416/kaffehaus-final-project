package com.maestria.kaffehausv4.Shared.Presentation;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.maestria.kaffehausv4.Shared.Domain.Error.BaseException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        return construirRespuesta(ex.getCodigo(), ex.getTipo(), ex.getMensaje());
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorResponse> handleSystemException(SystemException ex) {
        return construirRespuesta(ex.getCodigo(), ex.getTipo(), ex.getMensaje());
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        return construirRespuesta(ex.getCodigo(), ex.getTipo(), ex.getMensaje());
    }

    /**
     * Interbloqueo o espera de bloqueo abortada por la base (ventas.md VEN-65, inventario.md
     * INV-53). La transaccion ya revirtio entera, asi que el cliente puede reintentar: es un
     * conflicto, no un fallo del servidor.
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(PessimisticLockingFailureException ex) {
        return construirRespuesta(
            HttpStatus.CONFLICT.value(),
            "Domain Exception",
            "La operacion no se pudo completar por concurrencia; reintenta"
        );
    }

    /**
     * Dos indices unicos guardan reglas de negocio frente a peticiones simultaneas, y chocar con
     * ellos es un conflicto, no un fallo del servidor:
     *
     * - uq_venta_clave_idempotencia (ventas.md VEN-66): dos altas con la misma clave; la segunda
     *   revierte y al reintentar recibe la venta de la primera.
     * - uq_sesion_caja_abierta (arquitectura.md 4.11, CAJ-20): dos aperturas de caja a la vez; solo una
     *   queda abierta.
     *
     * Otras violaciones de integridad siguen siendo un fallo del servidor.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause().getMessage();
        if (detalle != null && detalle.contains("uq_venta_clave_idempotencia")) {
            return construirRespuesta(
                HttpStatus.CONFLICT.value(),
                "Domain Exception",
                "Ya se esta registrando una venta con esa clave de idempotencia; reintenta"
            );
        }
        if (detalle != null && detalle.contains("uq_sesion_caja_abierta")) {
            return construirRespuesta(
                HttpStatus.CONFLICT.value(),
                "Domain Exception",
                "Ya hay una caja abierta; cierrala antes de abrir otra"
            );
        }
        return handleRuntimeException(ex);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return construirRespuesta(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Runtime Exception",
            ex.getMessage() != null ? ex.getMessage() : "Ocurrio un error inesperado"
        );
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(int codigo, String tipo, String mensaje) {
        HttpStatus status = HttpStatus.resolve(codigo);
        HttpStatus httpStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse body = new ErrorResponse(codigo, tipo, mensaje, OffsetDateTime.now());
        return ResponseEntity.status(httpStatus).body(body);
    }

    public record ErrorResponse(int codigo, String tipo, String mensaje, OffsetDateTime timestamp) {}
}
