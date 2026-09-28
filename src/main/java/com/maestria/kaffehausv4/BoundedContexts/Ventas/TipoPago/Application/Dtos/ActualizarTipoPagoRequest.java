package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarTipoPagoRequest {
    private String nombre;

    private ActualizarTipoPagoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarTipoPagoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarTipoPagoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
