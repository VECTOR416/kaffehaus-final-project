package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;

public class EntidadResponse {
    private String id;
    private String nombre;
    private String nit;
    private String telefono;
    private String descripcion;
    private boolean borrado;

    private EntidadResponse(
        String id,
        String nombre,
        String nit,
        String telefono,
        String descripcion,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
        this.telefono = telefono;
        this.descripcion = descripcion;
        this.borrado = borrado;
    }

    public static EntidadResponse createFromAggregate(Entidad entidad){
        return new EntidadResponse(
            entidad.getId().getValue(),
            entidad.getNombre(),
            entidad.getNit(),
            entidad.getTelefono(),
            entidad.getDescripcion(),
            entidad.isBorrado()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public String getNit() {return nit;}
    public void setNit(String nit) {this.nit = nit;}
    public String getTelefono() {return telefono;}
    public void setTelefono(String telefono) {this.telefono = telefono;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
