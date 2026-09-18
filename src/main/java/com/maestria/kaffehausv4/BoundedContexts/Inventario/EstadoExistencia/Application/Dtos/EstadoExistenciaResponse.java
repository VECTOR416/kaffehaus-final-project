package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;

public class EstadoExistenciaResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private EstadoExistenciaResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoExistenciaResponse createFromAggregate(EstadoExistencia estadoExistencia){
        return new EstadoExistenciaResponse(
            estadoExistencia.getId().getValue(),
            estadoExistencia.getNombre(),
            estadoExistencia.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
