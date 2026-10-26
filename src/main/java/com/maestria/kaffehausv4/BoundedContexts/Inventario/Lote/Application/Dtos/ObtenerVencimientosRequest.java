package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Alerta de vencimiento: lo vencido y lo que vence en los proximos 'dias' (logica.md VENCIMIENTO-04). */
public class ObtenerVencimientosRequest {
    private static final int DIAS_POR_DEFECTO = 3;

    private int dias;

    private ObtenerVencimientosRequest(int dias) {
        this.dias = dias;
    }

    public static ObtenerVencimientosRequest createFromRequest(Map<String, Object> request) {
        Integer dias = Validador.enteroOpcional(request.get("dias"), "dias");
        if (dias != null && dias < 0) {
            throw DomainException.throwException(400, "El campo 'dias' no puede ser negativo");
        }
        return new ObtenerVencimientosRequest(dias != null ? dias : DIAS_POR_DEFECTO);
    }

    public int getDias() {return dias;}
}
