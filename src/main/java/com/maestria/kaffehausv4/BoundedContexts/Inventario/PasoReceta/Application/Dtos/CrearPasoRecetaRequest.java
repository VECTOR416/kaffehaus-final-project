package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearPasoRecetaRequest {
    private Integer pasos;
    private String descripcion;
    private String idReceta;
    private boolean borrado;

    private CrearPasoRecetaRequest(
        Integer pasos,
        String descripcion,
        String idReceta,
        boolean borrado
    ) {
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.idReceta = idReceta;
        this.borrado = borrado;
    }

    public static CrearPasoRecetaRequest createFromRequest(Map<String, Object> request){
        Integer pasos = Validador.enteroRequerido(request.get("pasos"), "pasos");
        String descripcion = Validador.textoRequerido(request.get("descripcion"), "descripcion");
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");

        return new CrearPasoRecetaRequest(pasos, descripcion, idReceta, false);
    }

    public Integer getPasos() {return pasos;}
    public void setPasos(Integer pasos) {this.pasos = pasos;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public String getIdReceta() {return idReceta;}
    public void setIdReceta(String idReceta) {this.idReceta = idReceta;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
