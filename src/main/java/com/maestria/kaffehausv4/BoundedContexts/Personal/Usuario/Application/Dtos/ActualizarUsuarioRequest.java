package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import java.util.Map;

public class ActualizarUsuarioRequest {
    private String idPersona;
    private String correo;
    private String contrasena;
    private String idRol;

    private ActualizarUsuarioRequest(
        String idPersona,
        String correo,
        String contrasena,
        String idRol
    ) {
        this.idPersona = idPersona;
        this.correo = correo;
        this.contrasena = contrasena;
        this.idRol = idRol;
    }

    public static ActualizarUsuarioRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String correo = (String) request.get("correo");
        String contrasena = (String) request.get("contrasena");
        String idRol = (String) request.get("idRol");

        return new ActualizarUsuarioRequest(idPersona, correo, contrasena, idRol);
    }

    public String getIdPersona() {return idPersona;}
    public void setIdPersona(String idPersona) {this.idPersona = idPersona;}
    public String getCorreo() {return correo;}
    public void setCorreo(String correo) {this.correo = correo;}
    public String getContrasena() {return contrasena;}
    public void setContrasena(String contrasena) {this.contrasena = contrasena;}
    public String getIdRol() {return idRol;}
    public void setIdRol(String idRol) {this.idRol = idRol;}
}
