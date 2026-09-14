package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;

public class TipoDescuentoResponse {
    private String id;
    private String nombre;

    private TipoDescuentoResponse(
        String id,
        String nombre
    ) {
        this.id = id;
        this.nombre = nombre;
    }

    public static TipoDescuentoResponse createFromAggregate(TipoDescuento tipoDescuento){
        return new TipoDescuentoResponse(
            tipoDescuento.getId().getValue(),
            tipoDescuento.getNombre()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}
