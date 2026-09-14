package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;

public class DetalleVentaResponse {
    private String id;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private boolean estado;
    private String idVenta;
    private String idProducto;

    private DetalleVentaResponse(
        String id,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        String idVenta,
        String idProducto
    ) {
        this.id = id;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
        this.idVenta = idVenta;
        this.idProducto = idProducto;
    }

    public static DetalleVentaResponse createFromAggregate(DetalleVenta detalleVenta){
        return new DetalleVentaResponse(
            detalleVenta.getId().getValue(),
            detalleVenta.getPrecioUnitario(),
            detalleVenta.getCantidad(),
            detalleVenta.getSubtotal(),
            detalleVenta.getDescuentoAplicado(),
            detalleVenta.isEstado(),
            detalleVenta.getVentaId() != null ? detalleVenta.getVentaId().getValue() : null,
            detalleVenta.getProductoId() != null ? detalleVenta.getProductoId().getValue() : null
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public void setSubtotal(BigDecimal subtotal) {this.subtotal = subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public void setDescuentoAplicado(BigDecimal descuentoAplicado) {this.descuentoAplicado = descuentoAplicado;}
    public boolean isEstado() {return estado;}
    public void setEstado(boolean estado) {this.estado = estado;}
    public String getIdVenta() {return idVenta;}
    public void setIdVenta(String idVenta) {this.idVenta = idVenta;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
}
