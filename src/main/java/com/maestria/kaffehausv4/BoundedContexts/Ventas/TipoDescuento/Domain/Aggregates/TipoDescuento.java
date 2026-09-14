package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

public class TipoDescuento {
    private TipoDescuentoId id;
    private String nombre;

    private TipoDescuento(
        TipoDescuentoId id,
        String nombre
    ){
        this.id = id;
        this.nombre = nombre;
    }

    public static TipoDescuento restaurar(
        String id,
        String nombre
    ){
        return new TipoDescuento(new TipoDescuentoId(id), nombre);
    }

    public static TipoDescuento crear(
        String nombre
    ){
        return new TipoDescuento(null, nombre);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public TipoDescuentoId getId() {return id;}
    public void setId(TipoDescuentoId id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
