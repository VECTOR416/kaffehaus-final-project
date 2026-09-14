package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearProduccionRequest {
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private String idReceta;
    private boolean borrado;

    private CrearProduccionRequest(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        String idReceta,
        boolean borrado
    ) {
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.idReceta = idReceta;
        this.borrado = borrado;
    }

    public static CrearProduccionRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidadEsperada = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadEsperada"), "cantidadEsperada"));
        BigDecimal cantidadReal = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadReal"), "cantidadReal"));
        BigDecimal diferencia = BigDecimal.valueOf(Validador.numeroRequerido(request.get("diferencia"), "diferencia"));
        String motivoDiferencia = Validador.textoOpcional(request.get("motivoDiferencia"), "motivoDiferencia");
        LocalDateTime fechaProduccion = Validador.fechaOpcional(request.get("fechaProduccion"), "fechaProduccion");
        fechaProduccion = fechaProduccion != null ? fechaProduccion : LocalDateTime.now();
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");

        return new CrearProduccionRequest(cantidadEsperada, cantidadReal, diferencia, motivoDiferencia, fechaProduccion, idReceta, false);
    }

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
