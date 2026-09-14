package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearPasoRecetaRequest {
    private Integer pasos;
    private String descripcion;
    private String idReceta;

    private CrearPasoRecetaRequest(
        Integer pasos,
        String descripcion,
        String idReceta
    ) {
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.idReceta = idReceta;
    }

    public static CrearPasoRecetaRequest createFromRequest(Map<String, Object> request){
        Integer pasos = Validador.enteroRequerido(request.get("pasos"), "pasos");
        String descripcion = Validador.textoRequerido(request.get("descripcion"), "descripcion");
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");

        return new CrearPasoRecetaRequest(pasos, descripcion, idReceta);
    }

    public Integer getPasos() {return pasos;}
    public String getDescripcion() {return descripcion;}
    public String getIdReceta() {return idReceta;}
}
