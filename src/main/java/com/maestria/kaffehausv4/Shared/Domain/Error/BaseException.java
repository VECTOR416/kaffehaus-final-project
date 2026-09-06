package com.maestria.kaffehausv4.Shared.Domain.Error;

public abstract class BaseException extends RuntimeException {

    private final int codigo;
    private final String mensaje;
    private final String tipo;

    public BaseException(int codigo, String mensaje, String tipo) {
        super(mensaje);
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.tipo = tipo;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getTipo() {
        return tipo;
    }
}
