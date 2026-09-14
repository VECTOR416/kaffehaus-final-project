package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class IngredienteReceta {
    private IngredienteRecetaId id;
    private BigDecimal cantidad;
    private RecetaId recetaId;
    private ProductoId productoId;
    private boolean borrado;

    private IngredienteReceta(
        IngredienteRecetaId id,
        BigDecimal cantidad,
        RecetaId recetaId,
        ProductoId productoId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.recetaId = recetaId;
        this.productoId = productoId;
        this.borrado = borrado;
    }

    public static IngredienteReceta restaurar(
        String id,
        BigDecimal cantidad,
        RecetaId recetaId,
        ProductoId productoId,
        boolean borrado
    ){
        return new IngredienteReceta(new IngredienteRecetaId(id), cantidad, recetaId, productoId, borrado);
    }

    public static IngredienteReceta crear(
        BigDecimal cantidad,
        RecetaId recetaId,
        ProductoId productoId
    ){
        return new IngredienteReceta(null, cantidad, recetaId, productoId, false);
    }

    public void actualizar(
        BigDecimal cantidad,
        RecetaId recetaId,
        ProductoId productoId
    ){
        this.cantidad = cantidad != null ? cantidad : this.cantidad;
        this.recetaId = recetaId != null ? recetaId : this.recetaId;
        this.productoId = productoId != null ? productoId : this.productoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public IngredienteRecetaId getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public RecetaId getRecetaId() {return recetaId;}
    public ProductoId getProductoId() {return productoId;}
    public boolean isBorrado() {return borrado;}
}
