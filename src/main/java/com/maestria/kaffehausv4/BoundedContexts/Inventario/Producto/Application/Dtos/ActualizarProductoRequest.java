package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarProductoRequest {
    private String nombre;
    private BigDecimal stock;
    private BigDecimal precio;
    private BigDecimal stockMinimo;
    private Boolean vendible;
    private String idUnidadActual;
    private String idTipo;
    private String idCategoria;
    private String idProductoPadre;

    private ActualizarProductoRequest(
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        Boolean vendible,
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
        this.idUnidadActual = idUnidadActual;
        this.idTipo = idTipo;
        this.idCategoria = idCategoria;
        this.idProductoPadre = idProductoPadre;
    }

    public static ActualizarProductoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");
        Double stockTmp = Validador.numeroOpcional(request.get("stock"), "stock");
        BigDecimal stock = stockTmp != null ? BigDecimal.valueOf(stockTmp) : null;
        Double precioTmp = Validador.numeroOpcional(request.get("precio"), "precio");
        BigDecimal precio = precioTmp != null ? BigDecimal.valueOf(precioTmp) : null;
        Double stockMinimoTmp = Validador.numeroOpcional(request.get("stockMinimo"), "stockMinimo");
        BigDecimal stockMinimo = stockMinimoTmp != null ? BigDecimal.valueOf(stockMinimoTmp) : null;
        Boolean vendibleTmp = Validador.booleanoOpcional(request.get("vendible"), "vendible");
        Boolean vendible = vendibleTmp;
        String idUnidadActual = Validador.textoOpcional(request.get("idUnidadActual"), "idUnidadActual");
        String idTipo = Validador.textoOpcional(request.get("idTipo"), "idTipo");
        String idCategoria = Validador.textoOpcional(request.get("idCategoria"), "idCategoria");
        String idProductoPadre = Validador.textoOpcional(request.get("idProductoPadre"), "idProductoPadre");

        return new ActualizarProductoRequest(nombre, stock, precio, stockMinimo, vendible, idUnidadActual, idTipo, idCategoria, idProductoPadre);
    }

    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public Boolean getVendible() {return vendible;}
    public String getIdUnidadActual() {return idUnidadActual;}
    public String getIdTipo() {return idTipo;}
    public String getIdCategoria() {return idCategoria;}
    public String getIdProductoPadre() {return idProductoPadre;}
}
