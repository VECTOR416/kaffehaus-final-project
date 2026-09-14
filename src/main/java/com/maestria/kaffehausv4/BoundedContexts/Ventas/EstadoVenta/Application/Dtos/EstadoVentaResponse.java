package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;

public class EstadoVentaResponse {
    private String id;
    private String nombre;

    private EstadoVentaResponse(
        String id,
        String nombre
    ) {
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoVentaResponse createFromAggregate(EstadoVenta estadoVenta){
        return new EstadoVentaResponse(
            estadoVenta.getId().getValue(),
            estadoVenta.getNombre()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
}
