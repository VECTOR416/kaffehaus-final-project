package com.maestria.kaffehausv4.Shared.Domain.Error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error de negocio o de validacion.
 *
 * 'error' y 'detalle' son opcionales y solo los llevan los errores estructurados que el cliente
 * debe poder resolver sin interpretar el mensaje (arquitectura.md 4.16): 'error' es el codigo
 * de negocio estable que define la regla de logica.md que lo provoca, y 'detalle' un payload
 * inmutable de datos simples, que conserva el orden de sus claves y admite valores nulos. En el
 * resto de errores son nulos.
 */
public class DomainException extends BaseException {

    private final String error;
    private final Map<String, Object> detalle;

    public DomainException(int codigo, String mensaje) {
        this(codigo, null, mensaje, null);
    }

    public DomainException(int codigo, String error, String mensaje, Map<String, Object> detalle) {
        super(codigo, mensaje, "Domain Exception");
        this.error = error;
        this.detalle = detalle != null ? Collections.unmodifiableMap(new LinkedHashMap<>(detalle)) : null;
    }

    public static DomainException throwException(int codigo, String mensaje) {
        return new DomainException(codigo, mensaje);
    }

    public static DomainException throwException(int codigo, String error, String mensaje, Map<String, Object> detalle) {
        return new DomainException(codigo, error, mensaje, detalle);
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getDetalle() {
        return detalle;
    }
}
