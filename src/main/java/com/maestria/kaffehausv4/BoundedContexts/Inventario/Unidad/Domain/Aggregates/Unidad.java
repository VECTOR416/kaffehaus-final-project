package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

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

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public UnidadId getId() {return id;}
    public void setId(UnidadId id) {this.id = id;}
    public String getUnidad() {return unidad;}
    public void setUnidad(String unidad) {this.unidad = unidad;}
    public String getAbreviatura() {return abreviatura;}
    public void setAbreviatura(String abreviatura) {this.abreviatura = abreviatura;}
    public boolean isEsBorrable() {return esBorrable;}
    public void setEsBorrable(boolean esBorrable) {this.esBorrable = esBorrable;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
