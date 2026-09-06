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
    public void setIdPersona(String idPersona) {this.idPersona = idPersona;}
    public String getNombres() {return nombres;}
    public void setNombres(String nombres) {this.nombres = nombres;}
    public String getApellidos() {return apellidos;}
    public void setApellidos(String apellidos) {this.apellidos = apellidos;}
    public String getTelefono() {return telefono;}
    public void setTelefono(String telefono) {this.telefono = telefono;}
    public String getCarnet() {return carnet;}
    public void setCarnet(String carnet) {this.carnet = carnet;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
