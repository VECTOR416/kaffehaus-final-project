package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;

public class TipoMovimientoResponse {
    private String id;
    private String nombre;
    private boolean borrado;

    private TipoMovimientoResponse(
        String id,
        String nombre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoMovimientoResponse createFromAggregate(TipoMovimiento tipoMovimiento){
        return new TipoMovimientoResponse(
            tipoMovimiento.getId().getValue(),
            tipoMovimiento.getNombre(),
            tipoMovimiento.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
