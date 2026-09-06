package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

public class Persona {
    private PersonaId id;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private boolean borrado;

    private Persona(
        PersonaId id,
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        boolean borrado
    ){
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.borrado = borrado;
    }

    public static Persona restaurar(
        String id,
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        boolean borrado
    ){
        return new Persona(
            new PersonaId(id), 
            nombres, 
            apellidos, 
            telefono, 
            carnet, 
            borrado
        );
    }

    public void eliminarPersona(){
        this.borrado = true;
    }

    public static Persona crear(
        String nombres,
        String apellidos,
        String telefono,
        String carnet
    ){
        return new Persona(
            null, 
            nombres, 
            apellidos, 
            telefono, 
            carnet, 
            false
        );
    }

    public void actualizar(
        String nombres,
        String apellidos,
        String telefono,
        String carnet
    ){
        this.nombres = nombres != null ? nombres : this.nombres;
        this.apellidos = apellidos != null ? apellidos : this.apellidos;
        this.telefono = telefono != null ? telefono : this.telefono;
        this.carnet = carnet != null ? carnet : this.carnet;
    }

    public PersonaId getId() {return id;}
    public void setId(PersonaId id) {this.id = id;}
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
