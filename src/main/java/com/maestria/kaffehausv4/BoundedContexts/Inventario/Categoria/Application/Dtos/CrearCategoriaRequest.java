package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearCategoriaRequest {
    private String nombre;

    private CrearCategoriaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearCategoriaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearCategoriaRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
