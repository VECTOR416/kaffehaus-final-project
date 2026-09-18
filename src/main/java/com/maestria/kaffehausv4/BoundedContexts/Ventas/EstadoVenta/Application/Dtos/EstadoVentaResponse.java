package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;

public class EstadoVentaResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private EstadoVentaResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoVentaResponse createFromAggregate(EstadoVenta estadoVenta){
        return new EstadoVentaResponse(
            estadoVenta.getId().getValue(),
            estadoVenta.getNombre(),
            estadoVenta.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
