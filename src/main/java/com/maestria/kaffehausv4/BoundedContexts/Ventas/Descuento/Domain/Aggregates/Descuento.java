package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

public class Descuento {
    private DescuentoId id;
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinal;
    private boolean estado;
    private ProductoId productoId;
    private TipoDescuentoId tipoDescuentoId;
    private boolean borrado;

    private Descuento(
        DescuentoId id,
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId,
        boolean borrado
    ){
        this.id = id;
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.estado = estado;
        this.productoId = productoId;
        this.tipoDescuentoId = tipoDescuentoId;
        this.borrado = borrado;
    }

    public static Descuento restaurar(
        String id,
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId,
        boolean borrado
    ){
        return new Descuento(new DescuentoId(id), descuento, fechaInicio, fechaFinal, estado, productoId, tipoDescuentoId, borrado);
    }

    public static Descuento crear(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId
    ){
        return new Descuento(null, descuento, fechaInicio, fechaFinal, estado, productoId, tipoDescuentoId, false);
    }

    public void actualizar(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        Boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId
    ){
        this.descuento = descuento != null ? descuento : this.descuento;
        this.fechaInicio = fechaInicio != null ? fechaInicio : this.fechaInicio;
        this.fechaFinal = fechaFinal != null ? fechaFinal : this.fechaFinal;
        this.estado = estado != null ? estado : this.estado;
        this.productoId = productoId != null ? productoId : this.productoId;
        this.tipoDescuentoId = tipoDescuentoId != null ? tipoDescuentoId : this.tipoDescuentoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public DescuentoId getId() {return id;}
    public void setId(DescuentoId id) {this.id = id;}
    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public void setFechaInicio(LocalDateTime fechaInicio) {this.fechaInicio = fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public void setFechaFinal(LocalDateTime fechaFinal) {this.fechaFinal = fechaFinal;}
    public boolean isEstado() {return estado;}
    public void setEstado(boolean estado) {this.estado = estado;}
    public ProductoId getProductoId() {return productoId;}
    public void setProductoId(ProductoId productoId) {this.productoId = productoId;}
    public TipoDescuentoId getTipoDescuentoId() {return tipoDescuentoId;}
    public void setTipoDescuentoId(TipoDescuentoId tipoDescuentoId) {this.tipoDescuentoId = tipoDescuentoId;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
