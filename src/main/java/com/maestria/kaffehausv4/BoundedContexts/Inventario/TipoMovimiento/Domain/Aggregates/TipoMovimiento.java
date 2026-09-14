package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;

public class TipoMovimiento {
    private TipoMovimientoId id;
    private String nombre;
    private boolean borrado;

    private TipoMovimiento(
        TipoMovimientoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoMovimiento restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new TipoMovimiento(new TipoMovimientoId(id), nombre, borrado);
    }

    public static TipoMovimiento crear(
        String nombre
    ){
        return new TipoMovimiento(null, nombre, false);
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

    public TipoMovimientoId getId() {return id;}
    public void setId(TipoMovimientoId id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
