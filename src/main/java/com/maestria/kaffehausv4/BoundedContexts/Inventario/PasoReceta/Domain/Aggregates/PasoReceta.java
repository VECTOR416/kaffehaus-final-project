package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class PasoReceta {
    private PasoRecetaId id;
    private Integer pasos;
    private String descripcion;
    private RecetaId recetaId;
    private boolean borrado;

    private PasoReceta(
        PasoRecetaId id,
        Integer pasos,
        String descripcion,
        RecetaId recetaId,
        boolean borrado
    ){
        this.id = id;
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.recetaId = recetaId;
        this.borrado = borrado;
    }

    public static PasoReceta restaurar(
        String id,
        Integer pasos,
        String descripcion,
        RecetaId recetaId,
        boolean borrado
    ){
        return new PasoReceta(new PasoRecetaId(id), pasos, descripcion, recetaId, borrado);
    }

    public static PasoReceta crear(
        Integer pasos,
        String descripcion,
        RecetaId recetaId
    ){
        return new PasoReceta(null, pasos, descripcion, recetaId, false);
    }

    public void actualizar(
        Integer pasos,
        String descripcion,
        RecetaId recetaId
    ){
        this.pasos = pasos != null ? pasos : this.pasos;
        this.descripcion = descripcion != null ? descripcion : this.descripcion;
        this.recetaId = recetaId != null ? recetaId : this.recetaId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public PasoRecetaId getId() {return id;}
    public Integer getPasos() {return pasos;}
    public String getDescripcion() {return descripcion;}
    public RecetaId getRecetaId() {return recetaId;}
    public boolean isBorrado() {return borrado;}
}
