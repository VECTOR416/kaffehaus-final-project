package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Linea del payload de una venta. No es un request de primer nivel: solo viaja anidada
 * dentro de CrearVentaRequest o ActualizarVentaRequest, porque un detalle no se registra
 * fuera de su venta.
 *
 * No admite 'subtotal': ese importe lo calcula el agregado a partir de precio, cantidad y
 * descuento, y aceptarlo del cliente abriria la puerta a un total incoherente.
 */
public class DetalleVentaRequest {
    private String idProducto;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal descuentoAplicado;

    private DetalleVentaRequest(
        String idProducto,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal descuentoAplicado
    ) {
        this.idProducto = idProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.descuentoAplicado = descuentoAplicado;
    }

    public static DetalleVentaRequest createFromRequest(Map<String, Object> request){
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        BigDecimal precioUnitario = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precioUnitario"), "precioUnitario"));
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        Double descuentoTmp = Validador.numeroOpcional(request.get("descuentoAplicado"), "descuentoAplicado");
        BigDecimal descuentoAplicado = descuentoTmp != null ? BigDecimal.valueOf(descuentoTmp) : BigDecimal.ZERO;

        return new DetalleVentaRequest(idProducto, precioUnitario, cantidad, descuentoAplicado);
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
}
