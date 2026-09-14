package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearUnidadRequest {
    private String unidad;
    private String abreviatura;
    private boolean esBorrable;
    private boolean borrado;

    private CrearUnidadRequest(
        String unidad,
        String abreviatura,
        boolean esBorrable,
        boolean borrado
    ) {
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.esBorrable = esBorrable;
        this.borrado = borrado;
    }

    public static CrearUnidadRequest createFromRequest(Map<String, Object> request){
        String unidad = Validador.textoRequerido(request.get("unidad"), "unidad");
        String abreviatura = Validador.textoOpcional(request.get("abreviatura"), "abreviatura");
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        boolean esBorrable = esBorrableTmp != null ? esBorrableTmp : true;

        return new CrearUnidadRequest(unidad, abreviatura, esBorrable, false);
    }

    public String getUnidad() {return unidad;}
    public void setUnidad(String unidad) {this.unidad = unidad;}
    public String getAbreviatura() {return abreviatura;}
    public void setAbreviatura(String abreviatura) {this.abreviatura = abreviatura;}
    public boolean isEsBorrable() {return esBorrable;}
    public void setEsBorrable(boolean esBorrable) {this.esBorrable = esBorrable;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
