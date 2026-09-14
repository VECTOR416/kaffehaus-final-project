package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarEstadoExistenciaRequest {
    private String nombre;

    private ActualizarEstadoExistenciaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarEstadoExistenciaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarEstadoExistenciaRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
