package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

public class UnidadResponse {
    private String id;
    private String unidad;
    private String abreviatura;
    private boolean esBorrable;
    private boolean borrado;

    private UnidadResponse(
        String id,
        String unidad,
        String abreviatura,
        boolean esBorrable,
        boolean borrado
    ) {
        this.id = id;
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.esBorrable = esBorrable;
        this.borrado = borrado;
    }

    public static UnidadResponse createFromAggregate(Unidad unidad){
        return new UnidadResponse(
            unidad.getId().getValue(),
            unidad.getUnidad(),
            unidad.getAbreviatura(),
            unidad.isEsBorrable(),
            unidad.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getUnidad() {return unidad;}
    public String getAbreviatura() {return abreviatura;}
    public boolean isEsBorrable() {return esBorrable;}
    public boolean isBorrado() {return borrado;}
}
