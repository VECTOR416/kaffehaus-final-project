package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import java.util.Map;

public class CrearUsuarioRequest {
    private String idPersona;
    private String correo;
    private String contrasena;
    private String idRol;
    private boolean borrado;

    private CrearUsuarioRequest(
        String idPersona,
        String correo,
        String contrasena,
        String idRol,
        boolean borrado
    ) {
        this.idPersona = idPersona;
        this.correo = correo;
        this.contrasena = contrasena;
        this.idRol = idRol;
        this.borrado = borrado;
    }

    public static CrearUsuarioRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String correo = (String) request.get("correo");
        String contrasena = (String) request.get("contrasena");
        String idRol = (String) request.get("idRol");

        return new CrearUsuarioRequest(idPersona, correo, contrasena, idRol, false);
    }

    public String getIdPersona() {return idPersona;}
    public String getCorreo() {return correo;}
    public String getContrasena() {return contrasena;}
    public String getIdRol() {return idRol;}
    public boolean isBorrado() {return borrado;}
}

