package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Linea del payload de alta de una venta: solo 'idProducto' y 'cantidad'. Viaja anidada
 * dentro de CrearVentaRequest.
 *
 * El precio, el descuento y el subtotal los pone el sistema (ventas.md VEN-13, VEN-14, VEN-15):
 * si llegan se rechazan en lugar de ignorarse, para que el POS no crea que cobro otro importe.
 */
public class DetalleVentaRequest {
    private static final List<String> CAMPOS_DERIVADOS = List.of("precioUnitario", "descuentoAplicado", "subtotal", "costoTotal");

    private String idProducto;
    private BigDecimal cantidad;

    private DetalleVentaRequest(String idProducto, BigDecimal cantidad) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public static DetalleVentaRequest createFromRequest(Map<String, Object> request){
        for (String campo : CAMPOS_DERIVADOS) {
            if (request.get(campo) != null) {
                throw DomainException.throwException(400,
                        "El campo '" + campo + "' de una linea de venta lo calcula el sistema; no se envia");
            }
        }
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));

        return new DetalleVentaRequest(idProducto, cantidad);
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
}
