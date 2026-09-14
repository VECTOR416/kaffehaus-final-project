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
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public boolean isEstado() {return estado;}
    public String getIdVenta() {return idVenta;}
    public String getIdProducto() {return idProducto;}
}
