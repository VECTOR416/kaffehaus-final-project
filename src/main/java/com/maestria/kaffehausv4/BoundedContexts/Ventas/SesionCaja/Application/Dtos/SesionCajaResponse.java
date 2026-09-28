package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;

public class SesionCajaResponse {
    private String id;
    private String idUsuario;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoApertura;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal montoCierreDeclarado;
    private BigDecimal montoCierreEsperado;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private String estado;
    private List<MovimientoCajaResponse> movimientos;

    private SesionCajaResponse(
        String id,
        String idUsuario,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal montoApertura,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal montoCierreDeclarado,
        BigDecimal montoCierreEsperado,
        BigDecimal diferencia,
        String motivoDiferencia,
        String estado,
        List<MovimientoCajaResponse> movimientos
    ) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.montoApertura = montoApertura;
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.montoCierreDeclarado = montoCierreDeclarado;
        this.montoCierreEsperado = montoCierreEsperado;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.estado = estado;
        this.movimientos = movimientos;
    }

    public static SesionCajaResponse createFromAggregate(SesionCaja sesionCaja) {
        return new SesionCajaResponse(
            sesionCaja.getId().getValue(),
            sesionCaja.getUsuarioId().getValue(),
            sesionCaja.getFechaApertura(),
            sesionCaja.getFechaCierre(),
            sesionCaja.getMontoApertura(),
            sesionCaja.getTotalIngresos(),
            sesionCaja.getTotalEgresos(),
            sesionCaja.getMontoCierreDeclarado(),
            sesionCaja.getMontoCierreEsperado(),
            sesionCaja.getDiferencia(),
            sesionCaja.getMotivoDiferencia(),
            sesionCaja.getEstado(),
            sesionCaja.getMovimientosActivos().stream()
                    .map(MovimientoCajaResponse::createFromAggregate)
                    .collect(Collectors.toList())
        );
    }

    public String getId() {return id;}
    public String getIdUsuario() {return idUsuario;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public LocalDateTime getFechaCierre() {return fechaCierre;}
    public BigDecimal getMontoApertura() {return montoApertura;}
    public BigDecimal getTotalIngresos() {return totalIngresos;}
    public BigDecimal getTotalEgresos() {return totalEgresos;}
    public BigDecimal getMontoCierreDeclarado() {return montoCierreDeclarado;}
    public BigDecimal getMontoCierreEsperado() {return montoCierreEsperado;}
    public BigDecimal getDiferencia() {return diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public String getEstado() {return estado;}
    public List<MovimientoCajaResponse> getMovimientos() {return movimientos;}
}
