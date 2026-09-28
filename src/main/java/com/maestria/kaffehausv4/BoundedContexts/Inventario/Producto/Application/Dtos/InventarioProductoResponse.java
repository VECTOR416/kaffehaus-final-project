package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;

/**
 * Situacion de inventario de un producto (inventario.md INV-30, INV-31, INV-46).
 *
 * - 'disponible': lo que se puede vender o consumir ya, sin abrir nada. En un MTS es el
 *   contenido abierto y vigente; en un MTO, las unidades que alcanzan sus insumos abiertos.
 * - 'disponibleTrasAbrir': lo mismo contando ademas las unidades cerradas no vencidas. Es
 *   informativo: avisa que hay stock por abrir (INV-13, INV-14).
 * - Los saldos fisicos (abierto, cerrado, total, valor) solo aplican a un MTS; en un MTO son
 *   nulos, porque no tiene stock propio.
 */
public class InventarioProductoResponse {
    private String idProducto;
    private String nombre;
    private boolean esStockDirecto;
    private String idUnidad;
    private BigDecimal stock;
    private BigDecimal stockMinimo;
    private boolean bajoMinimo;
    private BigDecimal stockAbierto;
    private BigDecimal stockCerrado;
    private BigDecimal stockTotal;
    private BigDecimal valorInventario;
    private BigDecimal disponible;
    private BigDecimal disponibleTrasAbrir;

    private InventarioProductoResponse(
        String idProducto,
        String nombre,
        boolean esStockDirecto,
        String idUnidad,
        BigDecimal stock,
        BigDecimal stockMinimo,
        boolean bajoMinimo,
        BigDecimal stockAbierto,
        BigDecimal stockCerrado,
        BigDecimal stockTotal,
        BigDecimal valorInventario,
        BigDecimal disponible,
        BigDecimal disponibleTrasAbrir
    ) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.esStockDirecto = esStockDirecto;
        this.idUnidad = idUnidad;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.bajoMinimo = bajoMinimo;
        this.stockAbierto = stockAbierto;
        this.stockCerrado = stockCerrado;
        this.stockTotal = stockTotal;
        this.valorInventario = valorInventario;
        this.disponible = disponible;
        this.disponibleTrasAbrir = disponibleTrasAbrir;
    }

    public static InventarioProductoResponse deStockDirecto(
            Producto producto,
            BigDecimal stockAbierto,
            BigDecimal stockCerrado,
            BigDecimal valorInventario,
            BigDecimal disponible,
            BigDecimal disponibleTrasAbrir) {
        return new InventarioProductoResponse(
                producto.getId().getValue(),
                producto.getNombre(),
                true,
                producto.getUnidadActualId().getValue(),
                producto.getStock(),
                producto.getStockMinimo(),
                producto.getStock().compareTo(producto.getStockMinimo()) < 0,
                stockAbierto,
                stockCerrado,
                stockAbierto.add(stockCerrado),
                valorInventario,
                disponible,
                disponibleTrasAbrir);
    }

    public static InventarioProductoResponse deBajoDemanda(Producto producto, BigDecimal disponible, BigDecimal disponibleTrasAbrir) {
        return new InventarioProductoResponse(
                producto.getId().getValue(),
                producto.getNombre(),
                false,
                producto.getUnidadActualId().getValue(),
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                disponible,
                disponibleTrasAbrir);
    }

    public String getIdProducto() {return idProducto;}
    public String getNombre() {return nombre;}
    public boolean isEsStockDirecto() {return esStockDirecto;}
    public String getIdUnidad() {return idUnidad;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isBajoMinimo() {return bajoMinimo;}
    public BigDecimal getStockAbierto() {return stockAbierto;}
    public BigDecimal getStockCerrado() {return stockCerrado;}
    public BigDecimal getStockTotal() {return stockTotal;}
    public BigDecimal getValorInventario() {return valorInventario;}
    public BigDecimal getDisponible() {return disponible;}
    public BigDecimal getDisponibleTrasAbrir() {return disponibleTrasAbrir;}
}
