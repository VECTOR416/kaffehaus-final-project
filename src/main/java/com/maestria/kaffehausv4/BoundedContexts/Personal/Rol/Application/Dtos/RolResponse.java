package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;

public class RolResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private RolResponse(String id, String nombre, boolean borrado) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static RolResponse createFromAggregate(Rol rol){
        return new RolResponse(
            rol.getId().getValue(),
            rol.getNombre(),
            rol.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
