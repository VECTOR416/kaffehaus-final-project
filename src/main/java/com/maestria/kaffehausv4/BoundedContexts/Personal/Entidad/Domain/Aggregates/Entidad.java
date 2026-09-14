package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;

public class Entidad {
    private EntidadId id;
    private String nombre;
    private String nit;
    private String telefono;
    private String descripcion;
    private boolean borrado;

    private Entidad(
        EntidadId id,
        String nombre,
        String nit,
        String telefono,
        String descripcion,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
        this.telefono = telefono;
        this.descripcion = descripcion;
        this.borrado = borrado;
    }

    public static Entidad restaurar(
        String id,
        String nombre,
        String nit,
        String telefono,
        String descripcion,
        boolean borrado
    ){
        return new Entidad(new EntidadId(id), nombre, nit, telefono, descripcion, borrado);
    }

    public static Entidad crear(
        String nombre,
        String nit,
        String telefono,
        String descripcion
    ){
        return new Entidad(null, nombre, nit, telefono, descripcion, false);
    }

    public void actualizar(
        String nombre,
        String nit,
        String telefono,
        String descripcion
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
        this.nit = nit != null ? nit : this.nit;
        this.telefono = telefono != null ? telefono : this.telefono;
        this.descripcion = descripcion != null ? descripcion : this.descripcion;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public EntidadId getId() {return id;}
    public String getNombre() {return nombre;}
    public String getNit() {return nit;}
    public String getTelefono() {return telefono;}
    public String getDescripcion() {return descripcion;}
    public boolean isBorrado() {return borrado;}
}
