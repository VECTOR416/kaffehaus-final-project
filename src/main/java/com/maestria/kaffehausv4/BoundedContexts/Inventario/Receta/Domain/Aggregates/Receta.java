package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class Receta {
    private RecetaId id;
    private BigDecimal cantidad;
    private String titulo;
    private ProductoId productoObtenidoId;
    private boolean borrado;

    private Receta(
        RecetaId id,
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.productoObtenidoId = productoObtenidoId;
        this.borrado = borrado;
    }

    public static Receta restaurar(
        String id,
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId,
        boolean borrado
    ){
        return new Receta(new RecetaId(id), cantidad, titulo, productoObtenidoId, borrado);
    }

    public static Receta crear(
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId
    ){
        return new Receta(null, cantidad, titulo, productoObtenidoId, false);
    }

    public void actualizar(
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId
    ){
        this.cantidad = cantidad != null ? cantidad : this.cantidad;
        this.titulo = titulo != null ? titulo : this.titulo;
        this.productoObtenidoId = productoObtenidoId != null ? productoObtenidoId : this.productoObtenidoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public RecetaId getId() {return id;}
    public void setId(RecetaId id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getTitulo() {return titulo;}
    public void setTitulo(String titulo) {this.titulo = titulo;}
    public ProductoId getProductoObtenidoId() {return productoObtenidoId;}
    public void setProductoObtenidoId(ProductoId productoObtenidoId) {this.productoObtenidoId = productoObtenidoId;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
