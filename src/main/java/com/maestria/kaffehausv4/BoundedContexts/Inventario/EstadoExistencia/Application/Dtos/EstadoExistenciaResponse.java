package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;

public class EstadoExistenciaResponse {
    private String id;
    private String nombre;

    private EstadoExistenciaResponse(
        String id,
        String nombre
    ) {
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoExistenciaResponse createFromAggregate(EstadoExistencia estadoExistencia){
        return new EstadoExistenciaResponse(
            estadoExistencia.getId().getValue(),
            estadoExistencia.getNombre()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
