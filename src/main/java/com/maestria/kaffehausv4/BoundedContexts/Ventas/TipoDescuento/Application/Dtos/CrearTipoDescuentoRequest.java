package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearTipoDescuentoRequest {
    private String nombre;

    private CrearTipoDescuentoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearTipoDescuentoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearTipoDescuentoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
