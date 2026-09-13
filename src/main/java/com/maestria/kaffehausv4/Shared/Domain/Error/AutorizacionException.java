package com.maestria.kaffehausv4.Shared.Domain.Error;

/**
 * Errores de autenticacion y autorizacion (401 / 403).
 *
 * Tercera hermana de DomainException y SystemException. Al extender BaseException,
 * el @ExceptionHandler(BaseException.class) que ya existe en GlobalExceptionHandler
 * la atiende sin necesidad de modificar esa clase.
 */
public class AutorizacionException extends BaseException {

    public AutorizacionException(int codigo, String mensaje) {
        super(codigo, mensaje, "Authorization Exception");
    }

    public static AutorizacionException throwException(int codigo, String mensaje) {
        return new AutorizacionException(codigo, mensaje);
    }
}
