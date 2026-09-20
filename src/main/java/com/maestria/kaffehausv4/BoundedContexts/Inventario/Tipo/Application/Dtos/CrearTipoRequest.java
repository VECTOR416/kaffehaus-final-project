package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearTipoRequest {
    private String nombre;

    private CrearTipoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearTipoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearTipoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
