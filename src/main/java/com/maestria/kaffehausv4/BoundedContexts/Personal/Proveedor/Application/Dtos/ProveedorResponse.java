package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Dtos.PersonaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;

public class ProveedorResponse {
    private String id;
    private PersonaResponse persona;
    private EntidadResponse entidad;
    private boolean borrado;

    private ProveedorResponse(
        String id,
        PersonaResponse persona,
        EntidadResponse entidad,
        boolean borrado
    ) {
        this.id = id;
        this.persona = persona;
        this.entidad = entidad;
        this.borrado = borrado;
    }

    public static ProveedorResponse createFromAggregate(Proveedor proveedor, Persona persona, Entidad entidad){
        PersonaResponse personaResponse = persona != null ? PersonaResponse.createFromAggregate(persona) : null;
        EntidadResponse entidadResponse = entidad != null ? EntidadResponse.createFromAggregate(entidad) : null;
        return new ProveedorResponse(
            proveedor.getId().getValue(),
            personaResponse,
            entidadResponse,
            proveedor.isBorrado()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public PersonaResponse getPersona() {return persona;}
    public void setPersona(PersonaResponse persona) {this.persona = persona;}
    public EntidadResponse getEntidad() {return entidad;}
    public void setEntidad(EntidadResponse entidad) {this.entidad = entidad;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
