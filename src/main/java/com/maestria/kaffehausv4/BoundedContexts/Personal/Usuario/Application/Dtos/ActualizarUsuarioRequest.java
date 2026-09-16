package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos;

import java.util.Map;

/**
 * No acepta 'idPersona': la persona asociada a un usuario no se reemplaza desde fuera,
 * se actualizan sus datos en sitio, igual que en ActualizarClienteRequest.
 */
public class ActualizarUsuarioRequest {
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private String correo;
    private String contrasena;
    private String idRol;

    private ActualizarUsuarioRequest(
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        String correo,
        String contrasena,
        String idRol
    ) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.correo = correo;
        this.contrasena = contrasena;
        this.idRol = idRol;
    }

    public static ActualizarUsuarioRequest createFromRequest(Map<String, Object> request){
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");
        String correo = (String) request.get("correo");
        String contrasena = (String) request.get("contrasena");
        String idRol = (String) request.get("idRol");

        return new ActualizarUsuarioRequest(nombres, apellidos, telefono, carnet, correo, contrasena, idRol);
    }

    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public String getCorreo() {return correo;}
    public String getContrasena() {return contrasena;}
    public String getIdRol() {return idRol;}
}
