package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;

public class TipoPagoResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private TipoPagoResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoPagoResponse createFromAggregate(TipoPago tipoPago){
        return new TipoPagoResponse(
            tipoPago.getId().getValue(),
            tipoPago.getNombre(),
            tipoPago.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
