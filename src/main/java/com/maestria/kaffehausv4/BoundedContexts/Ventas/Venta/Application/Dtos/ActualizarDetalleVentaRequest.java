package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Linea del payload de modificacion de una venta. Viaja anidada dentro de ActualizarVentaRequest.
 *
 * El campo 'id' decide la operacion: si viene, la linea existente se corrige; si no, se
 * incorpora una nueva y entonces 'idProducto', 'precioUnitario' y 'cantidad' pasan a ser
 * obligatorios (lo exige el caso de uso, que es quien conoce esa bifurcacion).
 *
 * No admite 'subtotal': ese importe lo calcula el agregado.
 */
public class ActualizarDetalleVentaRequest {
    private String id;
    private String idProducto;
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal descuentoAplicado;

    private ActualizarDetalleVentaRequest(
        String id,
        String idProducto,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal descuentoAplicado
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.descuentoAplicado = descuentoAplicado;
    }

    public static ActualizarDetalleVentaRequest createFromRequest(Map<String, Object> request){
        String id = Validador.textoOpcional(request.get("id"), "id");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");
        Double precioTmp = Validador.numeroOpcional(request.get("precioUnitario"), "precioUnitario");
        BigDecimal precioUnitario = precioTmp != null ? BigDecimal.valueOf(precioTmp) : null;
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        Double descuentoTmp = Validador.numeroOpcional(request.get("descuentoAplicado"), "descuentoAplicado");
        BigDecimal descuentoAplicado = descuentoTmp != null ? BigDecimal.valueOf(descuentoTmp) : null;

        return new ActualizarDetalleVentaRequest(id, idProducto, precioUnitario, cantidad, descuentoAplicado);
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
}
