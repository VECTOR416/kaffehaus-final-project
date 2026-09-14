package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class DetalleVenta {
    private DetalleVentaId id;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private boolean estado;
    private VentaId ventaId;
    private ProductoId productoId;

    private DetalleVenta(
        DetalleVentaId id,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        VentaId ventaId,
        ProductoId productoId
    ){
        this.id = id;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
        this.ventaId = ventaId;
        this.productoId = productoId;
    }

    public static DetalleVenta restaurar(
        String id,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        VentaId ventaId,
        ProductoId productoId
    ){
        return new DetalleVenta(new DetalleVentaId(id), precioUnitario, cantidad, subtotal, descuentoAplicado, estado, ventaId, productoId);
    }

    public static DetalleVenta crear(
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        VentaId ventaId,
        ProductoId productoId
    ){
        return new DetalleVenta(null, precioUnitario, cantidad, subtotal, descuentoAplicado, estado, ventaId, productoId);
    }

    public void actualizar(
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        Boolean estado,
        VentaId ventaId,
        ProductoId productoId
    ){
        this.precioUnitario = precioUnitario != null ? precioUnitario : this.precioUnitario;
        this.cantidad = cantidad != null ? cantidad : this.cantidad;
        this.subtotal = subtotal != null ? subtotal : this.subtotal;
        this.descuentoAplicado = descuentoAplicado != null ? descuentoAplicado : this.descuentoAplicado;
        this.estado = estado != null ? estado : this.estado;
        this.ventaId = ventaId != null ? ventaId : this.ventaId;
        this.productoId = productoId != null ? productoId : this.productoId;
    }

    public DetalleVentaId getId() {return id;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public boolean isEstado() {return estado;}
    public VentaId getVentaId() {return ventaId;}
    public ProductoId getProductoId() {return productoId;}
}
