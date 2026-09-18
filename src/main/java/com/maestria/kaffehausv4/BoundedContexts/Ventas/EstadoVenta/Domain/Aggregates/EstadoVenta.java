package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;

public class EstadoVenta {
    private EstadoVentaId id;
    private String nombre;
    private boolean borrado;

    private EstadoVenta(
        EstadoVentaId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoVenta restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new EstadoVenta(new EstadoVentaId(id), nombre, borrado);
    }

    public static EstadoVenta crear(
        String nombre
    ){
        return new EstadoVenta(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public EstadoVentaId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
