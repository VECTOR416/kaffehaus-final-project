package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarCategoriaRequest {
    private String nombre;
    private Boolean esBorrable;

    private ActualizarCategoriaRequest(
        String nombre,
        Boolean esBorrable
    ) {
        this.nombre = nombre;
        this.esBorrable = esBorrable;
    }

    public static ActualizarCategoriaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");
        Boolean esBorrable = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");

        return new ActualizarCategoriaRequest(nombre, esBorrable);
    }

    public String getNombre() {return nombre;}
    public Boolean getEsBorrable() {return esBorrable;}
}
