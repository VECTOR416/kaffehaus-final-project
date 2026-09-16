package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;

import java.util.Map;

/**
 * No acepta idPersona: la persona de contacto no se reemplaza desde fuera, se actualizan
 * sus datos en sitio, igual que en ActualizarClienteRequest. idEntidad si se puede
 * reapuntar, porque Entidad es un agregado con CRUD propio.
 */
public class ActualizarProveedorRequest {
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private String idEntidad;

    private ActualizarProveedorRequest(
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        String idEntidad
    ) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.idEntidad = idEntidad;
    }

    public static ActualizarProveedorRequest createFromRequest(Map<String, Object> request){
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");
        String idEntidad = (String) request.get("idEntidad");

        return new ActualizarProveedorRequest(nombres, apellidos, telefono, carnet, idEntidad);
    }

    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public String getIdEntidad() {return idEntidad;}
}
