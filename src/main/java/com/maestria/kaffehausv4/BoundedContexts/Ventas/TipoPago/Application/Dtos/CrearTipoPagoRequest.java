package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearTipoPagoRequest {
    private String nombre;

    private CrearTipoPagoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearTipoPagoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearTipoPagoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
