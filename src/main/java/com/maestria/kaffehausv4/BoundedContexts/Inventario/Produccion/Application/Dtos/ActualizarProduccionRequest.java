package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarProduccionRequest {
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private String idReceta;

    private ActualizarProduccionRequest(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        String idReceta
    ) {
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.idReceta = idReceta;
    }

    public static ActualizarProduccionRequest createFromRequest(Map<String, Object> request){
        Double cantidadEsperadaTmp = Validador.numeroOpcional(request.get("cantidadEsperada"), "cantidadEsperada");
        BigDecimal cantidadEsperada = cantidadEsperadaTmp != null ? BigDecimal.valueOf(cantidadEsperadaTmp) : null;
        Double cantidadRealTmp = Validador.numeroOpcional(request.get("cantidadReal"), "cantidadReal");
        BigDecimal cantidadReal = cantidadRealTmp != null ? BigDecimal.valueOf(cantidadRealTmp) : null;
        Double diferenciaTmp = Validador.numeroOpcional(request.get("diferencia"), "diferencia");
        BigDecimal diferencia = diferenciaTmp != null ? BigDecimal.valueOf(diferenciaTmp) : null;
        String motivoDiferencia = Validador.textoOpcional(request.get("motivoDiferencia"), "motivoDiferencia");
        LocalDateTime fechaProduccion = Validador.fechaOpcional(request.get("fechaProduccion"), "fechaProduccion");
        String idReceta = Validador.textoOpcional(request.get("idReceta"), "idReceta");

        return new ActualizarProduccionRequest(cantidadEsperada, cantidadReal, diferencia, motivoDiferencia, fechaProduccion, idReceta);
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
}
