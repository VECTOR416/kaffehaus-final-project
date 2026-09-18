package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Entidad interna del agregado Venta. NO es raiz de agregado.
 *
 * Se declara en el mismo paquete que Venta a proposito: es la senal mas fuerte de que su
 * ciclo de vida pertenece a la raiz. Sus factorias son package-private, asi que nadie fuera
 * de Venta puede instanciar una linea suelta; el unico camino es Venta.agregarDetalle(...).
 *
 * No guarda el VentaId del padre: dentro del agregado la relacion es la contencion en la
 * coleccion de Venta, y la columna 'detalle_venta.id_venta' la resuelve el mapper a partir
 * de la raiz al persistir.
 */
public class DetalleVenta {

    /** Escala de NUMERIC(12, 2) en 'detalle_venta'. Todo importe se redondea a este valor. */
    static final int ESCALA_MONETARIA = 2;

    private DetalleVentaId id;
    private ProductoId productoId;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private boolean estado;

    private DetalleVenta(
        DetalleVentaId id,
        ProductoId productoId,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado
    ){
        this.id = id;
        this.productoId = productoId;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
    }

    /**
     * Reconstruye una linea persistida. La invoca el mapper de infraestructura a traves de
     * Venta.restaurar, nunca la capa de aplicacion.
     */
    public static DetalleVenta restaurar(
        String id,
        ProductoId productoId,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado
    ){
        return new DetalleVenta(
                new DetalleVentaId(id),
                productoId,
                precioUnitario,
                cantidad,
                subtotal,
                descuentoAplicado,
                estado);
    }

    /**
     * Crea una linea nueva validando sus invariantes y calculando el subtotal.
     * Package-private: solo Venta puede llamarla.
     */
    static DetalleVenta crear(
        ProductoId productoId,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal descuentoAplicado
    ){
        if (productoId == null || productoId.getValue() == null) {
            throw DomainException.throwException(400, "El detalle de venta requiere un producto");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw DomainException.throwException(400, "El precio unitario del detalle de venta no puede ser negativo");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad del detalle de venta debe ser mayor a cero");
        }

        BigDecimal descuento = descuentoAplicado != null ? descuentoAplicado : BigDecimal.ZERO;
        if (descuento.signum() < 0) {
            throw DomainException.throwException(400, "El descuento aplicado del detalle de venta no puede ser negativo");
        }

        BigDecimal importeBruto = precioUnitario.multiply(cantidad);
        if (descuento.compareTo(importeBruto) > 0) {
            throw DomainException.throwException(400, "El descuento aplicado no puede superar el importe del detalle de venta");
        }

        return new DetalleVenta(
                null,
                productoId,
                aEscalaMonetaria(precioUnitario),
                cantidad,
                aEscalaMonetaria(importeBruto.subtract(descuento)),
                aEscalaMonetaria(descuento),
                true);
    }

    private static BigDecimal aEscalaMonetaria(BigDecimal valor) {
        return valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public DetalleVentaId getId() {return id;}
    public ProductoId getProductoId() {return productoId;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public boolean isEstado() {return estado;}
}
