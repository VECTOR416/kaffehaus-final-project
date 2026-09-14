package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarEstadoVentaRequest {
    private String nombre;

    private ActualizarEstadoVentaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarEstadoVentaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarEstadoVentaRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
