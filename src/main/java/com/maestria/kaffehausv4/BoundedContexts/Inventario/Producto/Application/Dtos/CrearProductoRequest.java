package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearProductoRequest {
    private String nombre;
    private BigDecimal stock;
    private BigDecimal precio;
    private BigDecimal stockMinimo;
    private boolean vendible;
    private String idUnidadActual;
    private String idCategoria;
    private String idProductoPadre;
    private boolean borrado;

    private CrearProductoRequest(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        String idUnidadActual,
        String idCategoria,
        String idProductoPadre,
        boolean borrado
    ) {
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.idUnidadActual = idUnidadActual;
        this.idCategoria = idCategoria;
        this.idProductoPadre = idProductoPadre;
        this.borrado = borrado;
    }

    public static CrearProductoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");
        BigDecimal stock = BigDecimal.valueOf(Validador.numeroRequerido(request.get("stock"), "stock"));
        BigDecimal precio = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precio"), "precio"));
        BigDecimal stockMinimo = BigDecimal.valueOf(Validador.numeroRequerido(request.get("stockMinimo"), "stockMinimo"));
        Boolean vendibleTmp = Validador.booleanoOpcional(request.get("vendible"), "vendible");
        boolean vendible = vendibleTmp != null ? vendibleTmp : true;
        String idUnidadActual = Validador.textoRequerido(request.get("idUnidadActual"), "idUnidadActual");
        String idCategoria = Validador.textoRequerido(request.get("idCategoria"), "idCategoria");
        String idProductoPadre = Validador.textoOpcional(request.get("idProductoPadre"), "idProductoPadre");

        return new CrearProductoRequest(nombre, stock, precio, stockMinimo, vendible, idUnidadActual, idCategoria, idProductoPadre, false);
    }

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public BigDecimal getStock() {return stock;}
    public void setStock(BigDecimal stock) {this.stock = stock;}
    public BigDecimal getPrecio() {return precio;}
    public void setPrecio(BigDecimal precio) {this.precio = precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public void setStockMinimo(BigDecimal stockMinimo) {this.stockMinimo = stockMinimo;}
    public boolean isVendible() {return vendible;}
    public void setVendible(boolean vendible) {this.vendible = vendible;}
    public String getIdUnidadActual() {return idUnidadActual;}
    public void setIdUnidadActual(String idUnidadActual) {this.idUnidadActual = idUnidadActual;}
    public String getIdCategoria() {return idCategoria;}
    public void setIdCategoria(String idCategoria) {this.idCategoria = idCategoria;}
    public String getIdProductoPadre() {return idProductoPadre;}
    public void setIdProductoPadre(String idProductoPadre) {this.idProductoPadre = idProductoPadre;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
