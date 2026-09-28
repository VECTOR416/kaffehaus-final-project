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
    private boolean esStockDirecto;
    private String idUnidadActual;
    private String idTipo;
    private String idCategoria;
    private String idProductoPadre;

    private CrearProductoRequest(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        boolean esStockDirecto,
        String idUnidadActual,
        String idTipo,
        String idCategoria,
        String idProductoPadre
    ) {
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.esStockDirecto = esStockDirecto;
        this.idUnidadActual = idUnidadActual;
        this.idTipo = idTipo;
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
        Boolean esStockDirectoTmp = Validador.booleanoOpcional(request.get("esStockDirecto"), "esStockDirecto");
        boolean esStockDirecto = esStockDirectoTmp != null ? esStockDirectoTmp : true;
        String idUnidadActual = Validador.textoRequerido(request.get("idUnidadActual"), "idUnidadActual");
        String idTipo = Validador.textoRequerido(request.get("idTipo"), "idTipo");
        String idCategoria = Validador.textoRequerido(request.get("idCategoria"), "idCategoria");
        String idProductoPadre = Validador.textoOpcional(request.get("idProductoPadre"), "idProductoPadre");

        return new CrearProductoRequest(nombre, stock, precio, stockMinimo, vendible, esStockDirecto, idUnidadActual, idTipo, idCategoria, idProductoPadre);
    }

    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isVendible() {return vendible;}
    public boolean isEsStockDirecto() {return esStockDirecto;}
    public String getIdUnidadActual() {return idUnidadActual;}
    public String getIdTipo() {return idTipo;}
    public String getIdCategoria() {return idCategoria;}
    public String getIdProductoPadre() {return idProductoPadre;}
}
