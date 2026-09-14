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
    public void setId(String id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
