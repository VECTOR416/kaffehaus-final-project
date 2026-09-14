package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos;

import java.util.Map;

public class CrearClienteRequest {
    private String idPersona;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private boolean borrado;

    private CrearClienteRequest(
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
    }

    public static CrearClienteRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");

        return new CrearClienteRequest(idPersona, nombres, apellidos, telefono, carnet, false);
    }

    public String getIdPersona() {return idPersona;}
    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public boolean isBorrado() {return borrado;}
}
