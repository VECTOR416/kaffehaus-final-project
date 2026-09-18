package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;

public class EstadoExistencia {
    private EstadoExistenciaId id;
    private String nombre;
    private boolean borrado;

    private EstadoExistencia(
        EstadoExistenciaId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoExistencia restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new EstadoExistencia(new EstadoExistenciaId(id), nombre, borrado);
    }

    public static EstadoExistencia crear(
        String nombre
    ){
        return new EstadoExistencia(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public EstadoExistenciaId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
