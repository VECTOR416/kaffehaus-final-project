package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;

public class Rol {
    private RolId id;
    private String nombre;
    private boolean borrado;

    private Rol(RolId id, String nombre, boolean borrado){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static Rol restaurar(String id, String nombre, boolean borrado){
        return new Rol(new RolId(id), nombre, borrado);
    }

    public static Rol crear(String nombre){
        return new Rol(null, nombre, false);
    }

    public void actualizar(String nombre){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public RolId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
