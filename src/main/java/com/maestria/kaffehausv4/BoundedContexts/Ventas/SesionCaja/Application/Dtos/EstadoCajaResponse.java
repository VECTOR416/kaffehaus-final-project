package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;

/**
 * Si hay un turno abierto y, en ese caso, desde cuando, quien responde por el y si se abrio un dia
 * anterior (logica.md SESIONCAJA-08, SESIONCAJA-17).
 */
public class EstadoCajaResponse {
    private boolean abierta;
    private String idSesionCaja;
    private LocalDateTime fechaApertura;
    private BigDecimal montoApertura;
    private String idResponsable;
    private String nombreResponsable;
    private boolean abiertaDesdeDiaAnterior;

    private EstadoCajaResponse(
        boolean abierta,
        String idSesionCaja,
        LocalDateTime fechaApertura,
        BigDecimal montoApertura,
        String idResponsable,
        String nombreResponsable,
        boolean abiertaDesdeDiaAnterior
    ) {
        this.abierta = abierta;
        this.idSesionCaja = idSesionCaja;
        this.fechaApertura = fechaApertura;
        this.montoApertura = montoApertura;
        this.idResponsable = idResponsable;
        this.nombreResponsable = nombreResponsable;
        this.abiertaDesdeDiaAnterior = abiertaDesdeDiaAnterior;
    }

    /** Con 'sesionCaja' nula describe una caja cerrada. */
    public static EstadoCajaResponse createFromAggregate(SesionCaja sesionCaja, String nombreResponsable, LocalDate hoy) {
        if (sesionCaja == null) {
            return new EstadoCajaResponse(false, null, null, null, null, null, false);
        }
        return new EstadoCajaResponse(
            true,
            sesionCaja.getId().getValue(),
            sesionCaja.getFechaApertura(),
            sesionCaja.getMontoApertura(),
            sesionCaja.getUsuarioId().getValue(),
            nombreResponsable,
            sesionCaja.abiertaDesdeDiaAnterior(hoy)
        );
    }

    public boolean isAbierta() {return abierta;}
    public String getIdSesionCaja() {return idSesionCaja;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public BigDecimal getMontoApertura() {return montoApertura;}
    public String getIdResponsable() {return idResponsable;}
    public String getNombreResponsable() {return nombreResponsable;}
    public boolean isAbiertaDesdeDiaAnterior() {return abiertaDesdeDiaAnterior;}
}
