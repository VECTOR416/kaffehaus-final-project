package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;

import java.util.Map;

/**
 * Todo proveedor tiene una persona de contacto: la columna proveedor.id_persona es NOT
 * NULL. Por eso la peticion trae siempre sus datos personales, y el caso de uso la crea
 * en la misma transaccion; idPersona es opcional y solo sirve para reutilizar una persona
 * que ya existe.
 *
 * idEntidad es aparte y opcional: vincula al proveedor con una empresa, y Entidad si
 * tiene CRUD propio.
 */
public class CrearProveedorRequest {
    private String idPersona;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String carnet;
    private String idEntidad;
    private boolean borrado;

    private CrearProveedorRequest(
        String idPersona,
        String nombres,
        String apellidos,
        String telefono,
        String carnet,
        String idEntidad,
        boolean borrado
    ) {
        this.idPersona = idPersona;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.carnet = carnet;
        this.idEntidad = idEntidad;
        this.borrado = borrado;
    }

    public static CrearProveedorRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String nombres = (String) request.get("nombres");
        String apellidos = (String) request.get("apellidos");
        String telefono = (String) request.get("telefono");
        String carnet = (String) request.get("carnet");
        String idEntidad = (String) request.get("idEntidad");

        return new CrearProveedorRequest(idPersona, nombres, apellidos, telefono, carnet, idEntidad, false);
    }

    public String getIdPersona() {return idPersona;}
    public String getNombres() {return nombres;}
    public String getApellidos() {return apellidos;}
    public String getTelefono() {return telefono;}
    public String getCarnet() {return carnet;}
    public String getIdEntidad() {return idEntidad;}
    public boolean isBorrado() {return borrado;}
}
