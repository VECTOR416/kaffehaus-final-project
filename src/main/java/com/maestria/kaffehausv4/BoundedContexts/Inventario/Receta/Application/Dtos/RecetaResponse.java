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
    public void setId(String id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getTitulo() {return titulo;}
    public void setTitulo(String titulo) {this.titulo = titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public void setIdProductoObtenido(String idProductoObtenido) {this.idProductoObtenido = idProductoObtenido;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
