package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class Unidad {
    private UnidadId id;
    private String unidad;
    private String abreviatura;
    private boolean esBorrable;
    private boolean borrado;

    private Unidad(
        UnidadId id,
        String unidad,
        String abreviatura,
        boolean esBorrable,
        boolean borrado
    ){
        this.id = id;
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.esBorrable = esBorrable;
        this.borrado = borrado;
    }

    public static Unidad restaurar(
        String id,
        String unidad,
        String abreviatura,
        boolean esBorrable,
        boolean borrado
    ){
        return new Unidad(new UnidadId(id), unidad, abreviatura, esBorrable, borrado);
    }

    public static Unidad crear(
        String unidad,
        String abreviatura,
        boolean esBorrable
    ){
        return new Unidad(null, unidad, abreviatura, esBorrable, false);
    }

    public void actualizar(
        String unidad,
        String abreviatura,
        Boolean esBorrable
    ){
        this.unidad = unidad != null ? unidad : this.unidad;
        this.abreviatura = abreviatura != null ? abreviatura : this.abreviatura;
        this.esBorrable = esBorrable != null ? esBorrable : this.esBorrable;
    }

    /** Un registro base marcado como no borrable no se da de baja (logica.md CATALOGO-INV-05). */
    public void eliminar(){
        if (!this.esBorrable) {
            throw DomainException.throwException(409, "La unidad con id '" + this.id.getValue() + "' no se puede dar de baja");
        }
        this.borrado = true;
    }

    public UnidadId getId() {return id;}
    public String getUnidad() {return unidad;}
    public String getAbreviatura() {return abreviatura;}
    public boolean isEsBorrable() {return esBorrable;}
    public boolean isBorrado() {return borrado;}
}
