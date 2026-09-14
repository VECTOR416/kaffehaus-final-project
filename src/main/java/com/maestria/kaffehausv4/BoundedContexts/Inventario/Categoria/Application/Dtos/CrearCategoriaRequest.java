package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearCategoriaRequest {
    private String nombre;
    private boolean borrado;

    private CrearCategoriaRequest(
        String nombre,
        boolean borrado
    ) {
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static CrearCategoriaRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearCategoriaRequest(nombre, false);
    }

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
