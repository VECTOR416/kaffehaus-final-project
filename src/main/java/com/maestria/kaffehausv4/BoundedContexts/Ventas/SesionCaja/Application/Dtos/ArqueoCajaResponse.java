package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;

/**
 * Resultado del cierre: la sesion cerrada, el arqueo del efectivo fisico y el desglose de lo
 * recaudado por cada tipo de pago. Solo cuentan las ventas completadas; las anuladas se
 * informan aparte y no suman al esperado.
 */
public class ArqueoCajaResponse {
    private SesionCajaResponse sesion;
    private BigDecimal montoApertura;
    private BigDecimal ventasEnEfectivo;
    private BigDecimal ingresosManuales;
    private BigDecimal egresosManuales;
    private BigDecimal montoCierreEsperado;
    private BigDecimal montoCierreDeclarado;
    private BigDecimal diferencia;
    private BigDecimal totalVendido;
    private long cantidadVentas;
    private long cantidadVentasAnuladas;
    private List<RecaudacionPorTipoPagoResponse> recaudacionPorTipoPago;
    private List<RecaudacionPorUsuarioResponse> recaudacionPorUsuario;

    private ArqueoCajaResponse(
        SesionCajaResponse sesion,
        BigDecimal montoApertura,
        BigDecimal ventasEnEfectivo,
        BigDecimal ingresosManuales,
        BigDecimal egresosManuales,
        BigDecimal montoCierreEsperado,
        BigDecimal montoCierreDeclarado,
        BigDecimal diferencia,
        BigDecimal totalVendido,
        long cantidadVentas,
        long cantidadVentasAnuladas,
        List<RecaudacionPorTipoPagoResponse> recaudacionPorTipoPago,
        List<RecaudacionPorUsuarioResponse> recaudacionPorUsuario
    ) {
        this.sesion = sesion;
        this.montoApertura = montoApertura;
        this.ventasEnEfectivo = ventasEnEfectivo;
        this.ingresosManuales = ingresosManuales;
        this.egresosManuales = egresosManuales;
        this.montoCierreEsperado = montoCierreEsperado;
        this.montoCierreDeclarado = montoCierreDeclarado;
        this.diferencia = diferencia;
        this.totalVendido = totalVendido;
        this.cantidadVentas = cantidadVentas;
        this.cantidadVentasAnuladas = cantidadVentasAnuladas;
        this.recaudacionPorTipoPago = recaudacionPorTipoPago;
        this.recaudacionPorUsuario = recaudacionPorUsuario;
    }

    public static ArqueoCajaResponse createFromAggregate(
        SesionCaja sesionCaja,
        BigDecimal ventasEnEfectivo,
        BigDecimal totalVendido,
        long cantidadVentas,
        long cantidadVentasAnuladas,
        List<RecaudacionPorTipoPagoResponse> recaudacionPorTipoPago,
        List<RecaudacionPorUsuarioResponse> recaudacionPorUsuario
    ) {
        return new ArqueoCajaResponse(
            SesionCajaResponse.createFromAggregate(sesionCaja),
            sesionCaja.getMontoApertura(),
            ventasEnEfectivo,
            sesionCaja.getTotalIngresos(),
            sesionCaja.getTotalEgresos(),
            sesionCaja.getMontoCierreEsperado(),
            sesionCaja.getMontoCierreDeclarado(),
            sesionCaja.getDiferencia(),
            totalVendido,
            cantidadVentas,
            cantidadVentasAnuladas,
            recaudacionPorTipoPago,
            recaudacionPorUsuario
        );
    }

    public SesionCajaResponse getSesion() {return sesion;}
    public BigDecimal getMontoApertura() {return montoApertura;}
    public BigDecimal getVentasEnEfectivo() {return ventasEnEfectivo;}
    public BigDecimal getIngresosManuales() {return ingresosManuales;}
    public BigDecimal getEgresosManuales() {return egresosManuales;}
    public BigDecimal getMontoCierreEsperado() {return montoCierreEsperado;}
    public BigDecimal getMontoCierreDeclarado() {return montoCierreDeclarado;}
    public BigDecimal getDiferencia() {return diferencia;}
    public BigDecimal getTotalVendido() {return totalVendido;}
    public long getCantidadVentas() {return cantidadVentas;}
    public long getCantidadVentasAnuladas() {return cantidadVentasAnuladas;}
    public List<RecaudacionPorTipoPagoResponse> getRecaudacionPorTipoPago() {return recaudacionPorTipoPago;}
    public List<RecaudacionPorUsuarioResponse> getRecaudacionPorUsuario() {return recaudacionPorUsuario;}
}
