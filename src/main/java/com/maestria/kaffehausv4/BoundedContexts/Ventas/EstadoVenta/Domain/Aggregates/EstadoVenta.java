package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;

public class EstadoVenta {
    private EstadoVentaId id;
    private String nombre;

    private EstadoVenta(
        EstadoVentaId id,
        String nombre
    ){
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoVenta restaurar(
        String id,
        String nombre
    ){
        return new EstadoVenta(new EstadoVentaId(id), nombre);
    }

    public static EstadoVenta crear(
        String nombre
    ){
        return new EstadoVenta(null, nombre);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public EstadoVentaId getId() {return id;}
    public void setId(EstadoVentaId id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
