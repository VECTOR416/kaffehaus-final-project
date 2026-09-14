package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearEstadoExistenciaRequest {
    private String nombre;

    private CrearEstadoExistenciaRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearEstadoExistenciaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearEstadoExistenciaRequest(nombre);
    }

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
