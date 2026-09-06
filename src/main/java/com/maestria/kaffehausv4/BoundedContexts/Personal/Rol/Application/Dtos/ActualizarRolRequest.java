package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos;

import java.util.Map;

public class ActualizarRolRequest {
    private String nombre;

    private ActualizarRolRequest(String nombre) {
        this.nombre = nombre;
    }

    public static ActualizarRolRequest createFromRequest(Map<String, Object> request){
        String nombre = (String) request.get("nombre");

        return new ActualizarRolRequest(nombre);
    }

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
