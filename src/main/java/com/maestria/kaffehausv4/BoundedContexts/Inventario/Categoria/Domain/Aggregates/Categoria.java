package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;

public class Categoria {
    private CategoriaId id;
    private String nombre;
    private boolean esBorrable;
    private boolean borrado;

    private Categoria(
        CategoriaId id,
        String nombre,
        boolean esBorrable,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.esBorrable = esBorrable;
        this.borrado = borrado;
    }

    public static Categoria restaurar(
        String id,
        String nombre,
        boolean esBorrable,
        boolean borrado
    ){
        return new Categoria(new CategoriaId(id), nombre, esBorrable, borrado);
    }

    public static Categoria crear(
        String nombre,
        boolean esBorrable
    ){
        return new Categoria(null, nombre, esBorrable, false);
    }

    public void actualizar(
        String nombre,
        Boolean esBorrable
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
        this.esBorrable = esBorrable != null ? esBorrable : this.esBorrable;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public CategoriaId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isEsBorrable() {return esBorrable;}
    public boolean isBorrado() {return borrado;}
}
