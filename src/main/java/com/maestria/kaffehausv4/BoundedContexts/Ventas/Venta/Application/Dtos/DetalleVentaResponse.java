package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;

/**
 * Linea de venta en la respuesta. Siempre viaja anidada dentro de VentaResponse, por eso no
 * repite el idVenta: el identificador de la venta ya esta en el nivel superior.
 */
public class DetalleVentaResponse {
    private String id;
    private String idProducto;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private boolean estado;
    private boolean borrado;

    private DetalleVentaResponse(
        String id,
        String idProducto,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        boolean borrado
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
        this.borrado = borrado;
    }

    public static DetalleVentaResponse createFromAggregate(DetalleVenta detalleVenta){
        return new DetalleVentaResponse(
            detalleVenta.getId() != null ? detalleVenta.getId().getValue() : null,
            detalleVenta.getProductoId() != null ? detalleVenta.getProductoId().getValue() : null,
            detalleVenta.getPrecioUnitario(),
            detalleVenta.getCantidad(),
            detalleVenta.getSubtotal(),
            detalleVenta.getDescuentoAplicado(),
            detalleVenta.isEstado(),
            detalleVenta.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public boolean isEstado() {return estado;}
    public boolean isBorrado() {return borrado;}
}
