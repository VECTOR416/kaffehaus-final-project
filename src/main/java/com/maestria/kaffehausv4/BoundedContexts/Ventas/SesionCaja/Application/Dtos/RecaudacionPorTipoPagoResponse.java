package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;

/** Lo cobrado con un tipo de pago en las ventas completadas de una sesion. */
public class RecaudacionPorTipoPagoResponse {
    private String idTipoPago;
    private String nombre;
    private BigDecimal monto;
    private long cantidadPagos;

    private RecaudacionPorTipoPagoResponse(String idTipoPago, String nombre, BigDecimal monto, long cantidadPagos) {
        this.idTipoPago = idTipoPago;
        this.nombre = nombre;
        this.monto = monto;
        this.cantidadPagos = cantidadPagos;
    }

    public static RecaudacionPorTipoPagoResponse crear(String idTipoPago, String nombre, BigDecimal monto, long cantidadPagos) {
        return new RecaudacionPorTipoPagoResponse(idTipoPago, nombre, monto, cantidadPagos);
    }

    public String getIdTipoPago() {return idTipoPago;}
    public String getNombre() {return nombre;}
    public BigDecimal getMonto() {return monto;}
    public long getCantidadPagos() {return cantidadPagos;}
}
