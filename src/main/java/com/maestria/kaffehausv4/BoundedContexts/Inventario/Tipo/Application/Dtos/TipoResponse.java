package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;

public class TipoResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private TipoResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoResponse createFromAggregate(Tipo tipo){
        return new TipoResponse(
            tipo.getId().getValue(),
            tipo.getNombre(),
            tipo.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
