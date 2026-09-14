package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;

public class EstadoExistencia {
    private EstadoExistenciaId id;
    private String nombre;

    private EstadoExistencia(
        EstadoExistenciaId id,
        String nombre
    ){
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoExistencia restaurar(
        String id,
        String nombre
    ){
        return new EstadoExistencia(new EstadoExistenciaId(id), nombre);
    }

    public static EstadoExistencia crear(
        String nombre
    ){
        return new EstadoExistencia(null, nombre);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public EstadoExistenciaId getId() {return id;}
    public String getNombre() {return nombre;}
}
