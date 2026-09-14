package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarPasoRecetaRequest {
    private Integer pasos;
    private String descripcion;
    private String idReceta;

    private ActualizarPasoRecetaRequest(
        Integer pasos,
        String descripcion,
        String idReceta
    ) {
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.idReceta = idReceta;
    }

    public static ActualizarPasoRecetaRequest createFromRequest(Map<String, Object> request){
        Integer pasos = Validador.enteroOpcional(request.get("pasos"), "pasos");
        String descripcion = Validador.textoOpcional(request.get("descripcion"), "descripcion");
        String idReceta = Validador.textoOpcional(request.get("idReceta"), "idReceta");

        return new ActualizarPasoRecetaRequest(pasos, descripcion, idReceta);
    }

    public Integer getPasos() {return pasos;}
    public void setPasos(Integer pasos) {this.pasos = pasos;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public String getIdReceta() {return idReceta;}
    public void setIdReceta(String idReceta) {this.idReceta = idReceta;}
}
