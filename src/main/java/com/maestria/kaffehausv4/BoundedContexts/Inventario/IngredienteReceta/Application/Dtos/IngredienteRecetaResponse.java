package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;

public class IngredienteRecetaResponse {
    private String id;
    private BigDecimal cantidad;
    private String idReceta;
    private String idProducto;
    private boolean borrado;

    private IngredienteRecetaResponse(
        String id,
        BigDecimal cantidad,
        String idReceta,
        String idProducto,
        boolean borrado
    ) {
        this.id = id;
        this.cantidad = cantidad;
        this.idReceta = idReceta;
        this.idProducto = idProducto;
        this.borrado = borrado;
    }

    public static IngredienteRecetaResponse createFromAggregate(IngredienteReceta ingredienteReceta){
        return new IngredienteRecetaResponse(
            ingredienteReceta.getId().getValue(),
            ingredienteReceta.getCantidad(),
            ingredienteReceta.getRecetaId() != null ? ingredienteReceta.getRecetaId().getValue() : null,
            ingredienteReceta.getProductoId() != null ? ingredienteReceta.getProductoId().getValue() : null,
            ingredienteReceta.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getIdReceta() {return idReceta;}
    public String getIdProducto() {return idProducto;}
    public boolean isBorrado() {return borrado;}
}
