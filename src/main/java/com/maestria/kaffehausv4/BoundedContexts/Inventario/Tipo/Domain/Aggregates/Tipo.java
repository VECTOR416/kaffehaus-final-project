package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;

public class Tipo {
    private TipoId id;
    private String nombre;
    private boolean borrado;

    private Tipo(
        TipoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static Tipo restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new Tipo(new TipoId(id), nombre, borrado);
    }

    public static Tipo crear(
        String nombre
    ){
        return new Tipo(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public TipoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
