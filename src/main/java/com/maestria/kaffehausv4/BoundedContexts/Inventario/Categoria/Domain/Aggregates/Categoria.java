package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;

public class Categoria {
    private CategoriaId id;
    private String nombre;
    private boolean borrado;

    private Categoria(
        CategoriaId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static Categoria restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new Categoria(new CategoriaId(id), nombre, borrado);
    }

    public static Categoria crear(
        String nombre
    ){
        return new Categoria(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public CategoriaId getId() {return id;}
    public void setId(CategoriaId id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
