package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearEstadoVentaRequest {
    private String nombre;

    private CrearEstadoVentaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearEstadoVentaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearEstadoVentaRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
