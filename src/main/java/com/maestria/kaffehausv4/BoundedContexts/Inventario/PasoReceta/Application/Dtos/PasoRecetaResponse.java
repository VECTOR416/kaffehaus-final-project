package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;

public class PasoRecetaResponse {
    private String id;
    private Integer pasos;
    private String descripcion;
    private String idReceta;
    private boolean borrado;

    private PasoRecetaResponse(
        String id,
        Integer pasos,
        String descripcion,
        String idReceta,
        boolean borrado
    ) {
        this.id = id;
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.idReceta = idReceta;
        this.borrado = borrado;
    }

    public static PasoRecetaResponse createFromAggregate(PasoReceta pasoReceta){
        return new PasoRecetaResponse(
            pasoReceta.getId().getValue(),
            pasoReceta.getPasos(),
            pasoReceta.getDescripcion(),
            pasoReceta.getRecetaId() != null ? pasoReceta.getRecetaId().getValue() : null,
            pasoReceta.isBorrado()
        );
    }

    public String getId() {return id;}
    public Integer getPasos() {return pasos;}
    public String getDescripcion() {return descripcion;}
    public String getIdReceta() {return idReceta;}
    public boolean isBorrado() {return borrado;}
}
