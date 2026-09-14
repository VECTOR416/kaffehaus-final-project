package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;

public class CategoriaResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private CategoriaResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static CategoriaResponse createFromAggregate(Categoria categoria){
        return new CategoriaResponse(
            categoria.getId().getValue(),
            categoria.getNombre(),
            categoria.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
