package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;

public class DetalleLote {
    private DetalleLoteId id;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private boolean fraccionable;
    private LoteId loteId;
    private ProductoId productoId;
    private boolean borrado;

    private DetalleLote(
        DetalleLoteId id,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        LoteId loteId,
        ProductoId productoId,
        boolean borrado
    ){
        this.id = id;
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.loteId = loteId;
        this.productoId = productoId;
        this.borrado = borrado;
    }

    public static DetalleLote restaurar(
        String id,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        LoteId loteId,
        ProductoId productoId,
        boolean borrado
    ){
        return new DetalleLote(new DetalleLoteId(id), cantidadInicial, cantidadRestante, precioUnitario, fraccionable, loteId, productoId, borrado);
    }

    public static DetalleLote crear(
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        LoteId loteId,
        ProductoId productoId
    ){
        return new DetalleLote(null, cantidadInicial, cantidadRestante, precioUnitario, fraccionable, loteId, productoId, false);
    }

    public void actualizar(
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        Boolean fraccionable,
        LoteId loteId,
        ProductoId productoId
    ){
        this.cantidadInicial = cantidadInicial != null ? cantidadInicial : this.cantidadInicial;
        this.cantidadRestante = cantidadRestante != null ? cantidadRestante : this.cantidadRestante;
        this.precioUnitario = precioUnitario != null ? precioUnitario : this.precioUnitario;
        this.fraccionable = fraccionable != null ? fraccionable : this.fraccionable;
        this.loteId = loteId != null ? loteId : this.loteId;
        this.productoId = productoId != null ? productoId : this.productoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public DetalleLoteId getId() {return id;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public boolean isFraccionable() {return fraccionable;}
    public LoteId getLoteId() {return loteId;}
    public ProductoId getProductoId() {return productoId;}
    public boolean isBorrado() {return borrado;}
}
