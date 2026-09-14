package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearUnidadRequest {
    private String unidad;
    private String abreviatura;
    private boolean esBorrable;

    private CrearUnidadRequest(
        String unidad,
        String abreviatura,
        boolean esBorrable
    ) {
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.esBorrable = esBorrable;
    }

    public static CrearUnidadRequest createFromRequest(Map<String, Object> request){
        String unidad = Validador.textoRequerido(request.get("unidad"), "unidad");
        String abreviatura = Validador.textoOpcional(request.get("abreviatura"), "abreviatura");
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        boolean esBorrable = esBorrableTmp != null ? esBorrableTmp : true;

        return new CrearUnidadRequest(unidad, abreviatura, esBorrable);
    }

    public String getUnidad() {return unidad;}
    public String getAbreviatura() {return abreviatura;}
    public boolean isEsBorrable() {return esBorrable;}
}
