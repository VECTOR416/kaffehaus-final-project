package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarUnidadRequest {
    private String unidad;
    private String abreviatura;
    private Boolean esBorrable;

    private ActualizarUnidadRequest(
        String unidad,
        String abreviatura,
        Boolean esBorrable
    ) {
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.esBorrable = esBorrable;
    }

    public static ActualizarUnidadRequest createFromRequest(Map<String, Object> request){
        String unidad = Validador.textoOpcional(request.get("unidad"), "unidad");
        String abreviatura = Validador.textoOpcional(request.get("abreviatura"), "abreviatura");
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        Boolean esBorrable = esBorrableTmp;

        return new ActualizarUnidadRequest(unidad, abreviatura, esBorrable);
    }

    public String getUnidad() {return unidad;}
    public String getAbreviatura() {return abreviatura;}
    public Boolean getEsBorrable() {return esBorrable;}
}
