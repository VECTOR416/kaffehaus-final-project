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
    public String getNombre() {return nombre;}
}
