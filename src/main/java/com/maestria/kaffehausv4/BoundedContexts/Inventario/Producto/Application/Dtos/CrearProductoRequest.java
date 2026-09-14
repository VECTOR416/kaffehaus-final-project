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

    private CrearProductoRequest(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        String idUnidadActual,
        String idCategoria,
        String idProductoPadre
    ) {
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.idUnidadActual = idUnidadActual;
        this.idCategoria = idCategoria;
        this.idProductoPadre = idProductoPadre;
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

        return new CrearProductoRequest(nombre, stock, precio, stockMinimo, vendible, idUnidadActual, idCategoria, idProductoPadre);
    }

    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isVendible() {return vendible;}
    public String getIdUnidadActual() {return idUnidadActual;}
    public String getIdCategoria() {return idCategoria;}
    public String getIdProductoPadre() {return idProductoPadre;}
}
