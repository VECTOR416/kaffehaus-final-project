package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import java.util.Map;

/**
 * 'idPersona' es opcional: si viene, el usuario se cuelga de una persona que ya existe
 * (alguien que ya es cliente, por ejemplo) y sus datos se refrescan; si no viene, el
 * caso de uso crea la persona con los campos de esta misma peticion. Persona no tiene
 * endpoint propio de alta, asi que esta es la unica via para darla de alta junto a un
 * usuario.
 */
public class CrearUsuarioRequest {
    private String idPersona;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private String correo;
    private String contrasena;
    private String idRol;
    private boolean borrado;

    private CrearUsuarioRequest(
        String idPersona,
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        String correo,
        String contrasena,
        String idRol,
        boolean borrado
    ) {
        this.idPersona = idPersona;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.correo = correo;
        this.contrasena = contrasena;
        this.idRol = idRol;
        this.borrado = borrado;
    }

    public static CrearUsuarioRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");
        String correo = (String) request.get("correo");
        String contrasena = (String) request.get("contrasena");
        String idRol = (String) request.get("idRol");

        return new CrearUsuarioRequest(idPersona, nombres, apellidos, telefono, carnet, correo, contrasena, idRol, false);
    }

    public String getIdPersona() {return idPersona;}
    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public String getCorreo() {return correo;}
    public String getContrasena() {return contrasena;}
    public String getIdRol() {return idRol;}
    public boolean isBorrado() {return borrado;}
}
