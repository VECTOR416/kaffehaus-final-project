package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos;

import java.util.Map;

public class CrearProveedorRequest {
    private String idPersona;
    private String idEntidad;
    private boolean borrado;

    private CrearProveedorRequest(
        String idPersona,
        String idEntidad,
        boolean borrado
    ) {
        this.idPersona = idPersona;
        this.idEntidad = idEntidad;
        this.borrado = borrado;
    }

    public static CrearProveedorRequest createFromRequest(Map<String, Object> request){
        String idPersona = (String) request.get("idPersona");
        String idEntidad = (String) request.get("idEntidad");

        return new CrearProveedorRequest(idPersona, idEntidad, false);
    }

    public String getIdPersona() {return idPersona;}
    public String getIdEntidad() {return idEntidad;}
    public boolean isBorrado() {return borrado;}
}
