package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

public class TipoDescuento {
    private TipoDescuentoId id;
    private String nombre;
    private boolean borrado;

    private TipoDescuento(
        TipoDescuentoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoDescuento restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new TipoDescuento(new TipoDescuentoId(id), nombre, borrado);
    }

    public static TipoDescuento crear(
        String nombre
    ){
        return new TipoDescuento(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public TipoDescuentoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
