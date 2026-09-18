package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;

/**
 * Ingrediente en la respuesta. Siempre viaja anidado dentro de RecetaResponse, por eso no
 * repite el idReceta: el identificador de la receta ya esta en el nivel superior.
 */
public class IngredienteRecetaResponse {
    private String id;
    private String idProducto;
    private BigDecimal cantidad;
    private boolean borrado;

    private IngredienteRecetaResponse(
        String id,
        String idProducto,
        BigDecimal cantidad,
        boolean borrado
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.borrado = borrado;
    }

    public static IngredienteRecetaResponse createFromAggregate(IngredienteReceta ingredienteReceta){
        return new IngredienteRecetaResponse(
            ingredienteReceta.getId() != null ? ingredienteReceta.getId().getValue() : null,
            ingredienteReceta.getProductoId() != null ? ingredienteReceta.getProductoId().getValue() : null,
            ingredienteReceta.getCantidad(),
            ingredienteReceta.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
    public boolean isBorrado() {return borrado;}
}
