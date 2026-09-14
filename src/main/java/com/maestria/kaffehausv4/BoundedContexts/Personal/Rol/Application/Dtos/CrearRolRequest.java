package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos;

import java.util.Map;

public class CrearRolRequest {
    private String nombre;
    private boolean borrado;

    private CrearRolRequest(String nombre, boolean borrado) {
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static CrearRolRequest createFromRequest(Map<String, Object> request){
        String nombre = (String) request.get("nombre");

        return new CrearRolRequest(nombre, false);
    }

    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
