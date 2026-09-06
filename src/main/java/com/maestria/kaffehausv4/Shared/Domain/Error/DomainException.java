package com.maestria.kaffehausv4.Shared.Domain.Error;

public class DomainException extends BaseException {

    public DomainException(int codigo, String mensaje) {
        super(codigo, mensaje, "Domain Exception");
    }

    public static DomainException throwException(int codigo, String mensaje) {
        return new DomainException(codigo, mensaje);
    }
}
