package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;

public class TipoDescuentoResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private TipoDescuentoResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoDescuentoResponse createFromAggregate(TipoDescuento tipoDescuento){
        return new TipoDescuentoResponse(
            tipoDescuento.getId().getValue(),
            tipoDescuento.getNombre(),
            tipoDescuento.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
