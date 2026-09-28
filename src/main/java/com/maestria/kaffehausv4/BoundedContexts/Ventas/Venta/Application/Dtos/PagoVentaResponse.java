package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;

/**
 * Pago en la respuesta. Viaja anidado dentro de la venta o del ticket. 'nombreTipoPago' solo
 * se completa cuando quien arma la respuesta resolvio el tipo (el ticket lo hace).
 */
public class PagoVentaResponse {
    private String id;
    private String idTipoPago;
    private String nombreTipoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private String referencia;

    private PagoVentaResponse(
        String id,
        String idTipoPago,
        String nombreTipoPago,
        BigDecimal monto,
        BigDecimal montoRecibido,
        BigDecimal cambio,
        String referencia
    ) {
        this.id = id;
        this.idTipoPago = idTipoPago;
        this.nombreTipoPago = nombreTipoPago;
        this.monto = monto;
        this.montoRecibido = montoRecibido;
        this.cambio = cambio;
        this.referencia = referencia;
    }

    public static PagoVentaResponse createFromAggregate(PagoVenta pago, TipoPago tipoPago) {
        return new PagoVentaResponse(
            pago.getId() != null ? pago.getId().getValue() : null,
            pago.getTipoPagoId().getValue(),
            tipoPago != null ? tipoPago.getNombre() : null,
            pago.getMonto(),
            pago.getMontoRecibido(),
            pago.getCambio(),
            pago.getReferencia()
        );
    }

    public String getId() {return id;}
    public String getIdTipoPago() {return idTipoPago;}
    public String getNombreTipoPago() {return nombreTipoPago;}
    public BigDecimal getMonto() {return monto;}
    public BigDecimal getMontoRecibido() {return montoRecibido;}
    public BigDecimal getCambio() {return cambio;}
    public String getReferencia() {return referencia;}
}
