package com.maestria.kaffehausv4.Shared.Presentation;

import java.time.OffsetDateTime;

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
