package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;

/**
 * Paso en la respuesta. Viaja anidado dentro de RecetaResponse y ya llega ordenado por su
 * numero de secuencia.
 */
public class PasoRecetaResponse {
    private String id;
    private Integer pasos;
    private String descripcion;
    private boolean borrado;

    private PasoRecetaResponse(
        String id,
        Integer pasos,
        String descripcion,
        boolean borrado
    ) {
        this.id = id;
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.borrado = borrado;
    }

    public static PasoRecetaResponse createFromAggregate(PasoReceta pasoReceta){
        return new PasoRecetaResponse(
            pasoReceta.getId() != null ? pasoReceta.getId().getValue() : null,
            pasoReceta.getPasos(),
            pasoReceta.getDescripcion(),
            pasoReceta.isBorrado()
        );
    }

    public String getId() {return id;}
    public Integer getPasos() {return pasos;}
    public String getDescripcion() {return descripcion;}
    public boolean isBorrado() {return borrado;}
}
