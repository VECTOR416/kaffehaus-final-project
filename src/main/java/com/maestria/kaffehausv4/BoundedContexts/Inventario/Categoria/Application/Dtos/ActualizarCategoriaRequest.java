package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarCategoriaRequest {
    private String nombre;

    private ActualizarCategoriaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarCategoriaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarCategoriaRequest(nombre);
    }

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
