package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

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

    /** Un registro base marcado como no borrable no se da de baja (logica.md CATALOGO-INV-05). */
    public void eliminar(){
        if (!this.esBorrable) {
            throw DomainException.throwException(409, "La categoria con id '" + this.id.getValue() + "' no se puede dar de baja");
        }
        this.borrado = true;
    }

    public CategoriaId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isEsBorrable() {return esBorrable;}
    public boolean isBorrado() {return borrado;}
}
