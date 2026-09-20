package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearCategoriaRequest {
    private String nombre;
    private boolean esBorrable;

    private CrearCategoriaRequest(
        String nombre,
        boolean esBorrable
    ) {
        this.nombre = nombre;
        this.esBorrable = esBorrable;
    }

    public static CrearCategoriaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        boolean esBorrable = esBorrableTmp != null ? esBorrableTmp : true;

        return new CrearCategoriaRequest(nombre, esBorrable);
    }

    public String getNombre() {return nombre;}
    public boolean isEsBorrable() {return esBorrable;}
}
