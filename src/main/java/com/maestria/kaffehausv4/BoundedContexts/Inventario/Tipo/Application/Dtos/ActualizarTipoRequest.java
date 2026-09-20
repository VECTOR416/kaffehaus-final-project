package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarTipoRequest {
    private String nombre;

    private ActualizarTipoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarTipoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarTipoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
