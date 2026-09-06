package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;

import java.util.Map;

public class ActualizarProveedorRequest {
    private String idPersona;
    private String idEntidad;

    private ActualizarProveedorRequest(
        String idPersona,
        String idEntidad
    ) {
        this.idPersona = idPersona;
        this.idEntidad = idEntidad;
    }

    public static ActualizarProveedorRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String idEntidad = (String) request.get("idEntidad");

        return new ActualizarProveedorRequest(idPersona, idEntidad);
    }

    public String getIdPersona() {return idPersona;}
    public void setIdPersona(String idPersona) {this.idPersona = idPersona;}
    public String getIdEntidad() {return idEntidad;}
    public void setIdEntidad(String idEntidad) {this.idEntidad = idEntidad;}
}
