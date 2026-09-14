package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;

public class RecetaResponse {
    private String id;
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;
    private boolean borrado;

    private RecetaResponse(
        String id,
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido,
        boolean borrado
    ) {
        this.id = id;
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
        this.borrado = borrado;
    }

    public static RecetaResponse createFromAggregate(Receta receta){
        return new RecetaResponse(
            receta.getId().getValue(),
            receta.getCantidad(),
            receta.getTitulo(),
            receta.getProductoObtenidoId() != null ? receta.getProductoObtenidoId().getValue() : null,
            receta.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public boolean isBorrado() {return borrado;}
}
