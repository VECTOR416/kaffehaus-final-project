package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class Producto {
    private ProductoId id;
    private String nombre;
    private BigDecimal stock;
    private BigDecimal precio;
    private BigDecimal stockMinimo;
    private boolean vendible;
    private UnidadId unidadActualId;
    private CategoriaId categoriaId;
    private ProductoId productoPadreId;
    private boolean borrado;

    private Producto(
        ProductoId id,
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        UnidadId unidadActualId,
        CategoriaId categoriaId,
        ProductoId productoPadreId,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.unidadActualId = unidadActualId;
        this.categoriaId = categoriaId;
        this.productoPadreId = productoPadreId;
        this.borrado = borrado;
    }

    public static Producto restaurar(
        String id,
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        UnidadId unidadActualId,
        CategoriaId categoriaId,
        ProductoId productoPadreId,
        boolean borrado
    ){
        return new Producto(new ProductoId(id), nombre, stock, precio, stockMinimo, vendible, unidadActualId, categoriaId, productoPadreId, borrado);
    }

    public static Producto crear(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        UnidadId unidadActualId,
        CategoriaId categoriaId,
        ProductoId productoPadreId
    ){
        return new Producto(null, nombre, stock, precio, stockMinimo, vendible, unidadActualId, categoriaId, productoPadreId, false);
    }

    public void actualizar(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        Boolean vendible,
        UnidadId unidadActualId,
        CategoriaId categoriaId,
        ProductoId productoPadreId
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
        this.stock = stock != null ? stock : this.stock;
        this.precio = precio != null ? precio : this.precio;
        this.stockMinimo = stockMinimo != null ? stockMinimo : this.stockMinimo;
        this.vendible = vendible != null ? vendible : this.vendible;
        this.unidadActualId = unidadActualId != null ? unidadActualId : this.unidadActualId;
        this.categoriaId = categoriaId != null ? categoriaId : this.categoriaId;
        this.productoPadreId = productoPadreId != null ? productoPadreId : this.productoPadreId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public ProductoId getId() {return id;}
    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isVendible() {return vendible;}
    public UnidadId getUnidadActualId() {return unidadActualId;}
    public CategoriaId getCategoriaId() {return categoriaId;}
    public ProductoId getProductoPadreId() {return productoPadreId;}
    public boolean isBorrado() {return borrado;}
}
