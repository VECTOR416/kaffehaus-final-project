package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;

public class ProduccionResponse {
    private String id;
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private String idReceta;
    private boolean borrado;

    private ProduccionResponse(
        String id,
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        String idReceta,
        boolean borrado
    ) {
        this.id = id;
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.idReceta = idReceta;
        this.borrado = borrado;
    }

    public static ProduccionResponse createFromAggregate(Produccion produccion){
        return new ProduccionResponse(
            produccion.getId().getValue(),
            produccion.getCantidadEsperada(),
            produccion.getCantidadReal(),
            produccion.getDiferencia(),
            produccion.getMotivoDiferencia(),
            produccion.getFechaProduccion(),
            produccion.getRecetaId() != null ? produccion.getRecetaId().getValue() : null,
            produccion.isBorrado()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public BigDecimal getCantidadEsperada() {return cantidadEsperada;}
    public void setCantidadEsperada(BigDecimal cantidadEsperada) {this.cantidadEsperada = cantidadEsperada;}
    public BigDecimal getCantidadReal() {return cantidadReal;}
    public void setCantidadReal(BigDecimal cantidadReal) {this.cantidadReal = cantidadReal;}
    public BigDecimal getDiferencia() {return diferencia;}
    public void setDiferencia(BigDecimal diferencia) {this.diferencia = diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public void setMotivoDiferencia(String motivoDiferencia) {this.motivoDiferencia = motivoDiferencia;}
    public LocalDateTime getFechaProduccion() {return fechaProduccion;}
    public void setFechaProduccion(LocalDateTime fechaProduccion) {this.fechaProduccion = fechaProduccion;}
    public String getIdReceta() {return idReceta;}
    public void setIdReceta(String idReceta) {this.idReceta = idReceta;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
