package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos;

import java.util.Map;

public class CrearEntidadRequest {
    private String nombre;
    private String nit;
    private String telefono;
    private String descripcion;
    private boolean borrado;

    private CrearEntidadRequest(
        String nombre,
        String nit,
        String telefono,
        String descripcion,
        boolean borrado
    ) {
        this.nombre = nombre;
        this.nit = nit;
        this.telefono = telefono;
        this.descripcion = descripcion;
        this.borrado = borrado;
    }

    public static CrearEntidadRequest createFromRequest(Map<String, Object> request){
        String nombre = (String) request.get("nombre");
        String nit = (String) request.get("nit");
        String telefono = (String) request.get("telefono");
        String descripcion = (String) request.get("descripcion");

        return new CrearEntidadRequest(nombre, nit, telefono, descripcion, false);
    }

    public String getNombre() {return nombre;}
    public String getNit() {return nit;}
    public String getTelefono() {return telefono;}
    public String getDescripcion() {return descripcion;}
    public boolean isBorrado() {return borrado;}
}
