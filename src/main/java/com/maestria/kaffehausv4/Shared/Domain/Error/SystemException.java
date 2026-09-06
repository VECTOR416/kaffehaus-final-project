package com.maestria.kaffehausv4.Shared.Domain.Error;

public class SystemException extends BaseException {

    public SystemException(int codigo, String mensaje) {
        super(codigo, mensaje, "System Exception");
    }

    public static SystemException throwException(int codigo, String mensaje) {
        return new SystemException(codigo, mensaje);
    }
}
