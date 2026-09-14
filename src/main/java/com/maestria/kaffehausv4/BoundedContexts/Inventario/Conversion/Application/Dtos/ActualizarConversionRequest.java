package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarConversionRequest {
    private BigDecimal factorConversion;
    private Boolean esBorrable;
    private String idUnidadOrigen;
    private String idUnidadDestino;

    private ActualizarConversionRequest(
        BigDecimal factorConversion,
        Boolean esBorrable,
        String idUnidadOrigen,
        String idUnidadDestino
    ) {
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.idUnidadOrigen = idUnidadOrigen;
        this.idUnidadDestino = idUnidadDestino;
    }

    public static ActualizarConversionRequest createFromRequest(Map<String, Object> request){
        Double factorConversionTmp = Validador.numeroOpcional(request.get("factorConversion"), "factorConversion");
        BigDecimal factorConversion = factorConversionTmp != null ? BigDecimal.valueOf(factorConversionTmp) : null;
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        Boolean esBorrable = esBorrableTmp;
        String idUnidadOrigen = Validador.textoOpcional(request.get("idUnidadOrigen"), "idUnidadOrigen");
        String idUnidadDestino = Validador.textoOpcional(request.get("idUnidadDestino"), "idUnidadDestino");

        return new ActualizarConversionRequest(factorConversion, esBorrable, idUnidadOrigen, idUnidadDestino);
    }

    public BigDecimal getFactorConversion() {return factorConversion;}
    public void setFactorConversion(BigDecimal factorConversion) {this.factorConversion = factorConversion;}
    public Boolean getEsBorrable() {return esBorrable;}
    public void setEsBorrable(Boolean esBorrable) {this.esBorrable = esBorrable;}
    public String getIdUnidadOrigen() {return idUnidadOrigen;}
    public void setIdUnidadOrigen(String idUnidadOrigen) {this.idUnidadOrigen = idUnidadOrigen;}
    public String getIdUnidadDestino() {return idUnidadDestino;}
    public void setIdUnidadDestino(String idUnidadDestino) {this.idUnidadDestino = idUnidadDestino;}
}
