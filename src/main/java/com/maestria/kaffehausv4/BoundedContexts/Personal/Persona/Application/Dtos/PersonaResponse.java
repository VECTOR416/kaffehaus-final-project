package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;

public class PersonaResponse {
    private String idPersona;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private boolean borrado;

    private PersonaResponse(
        String idPersona, 
        String nombres, 
        String apellidos, 
        String telefono, 
        String carnet,
        boolean borrado
    ) {
        this.idPersona = idPersona;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.borrado = borrado;
    }

    public static PersonaResponse createFromAggregate(Persona persona){
        return new PersonaResponse(
            persona.getId().getValue(), 
            persona.getNombres(), 
            persona.getApellidos(), 
            persona.getTelefono(), 
            persona.getCarnet(),
            persona.isBorrado()
        );
    }

    public String getIdPersona() {return idPersona;}
    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public boolean isBorrado() {return borrado;}
}
