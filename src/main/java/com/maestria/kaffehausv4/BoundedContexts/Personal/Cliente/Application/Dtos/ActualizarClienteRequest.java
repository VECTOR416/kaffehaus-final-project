package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos;

import java.util.Map;

public class ActualizarClienteRequest {
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;

    private ActualizarClienteRequest( 
        String nombres, 
        String apellidos, 
        String telefono, 
        String carnet
    ) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
    }

    public static ActualizarClienteRequest createFromRequest(Map<String, Object> request){
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");

        return new ActualizarClienteRequest(nombres, apellidos, telefono, carnet);
    }

    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
}
