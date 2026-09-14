package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearConversionRequest {
    private BigDecimal factorConversion;
    private boolean esBorrable;
    private String idUnidadOrigen;
    private String idUnidadDestino;

    private CrearConversionRequest(
        BigDecimal factorConversion,
        boolean esBorrable,
        String idUnidadOrigen,
        String idUnidadDestino
    ) {
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.idUnidadOrigen = idUnidadOrigen;
        this.idUnidadDestino = idUnidadDestino;
    }

    public static CrearConversionRequest createFromRequest(Map<String, Object> request){
        BigDecimal factorConversion = BigDecimal.valueOf(Validador.numeroRequerido(request.get("factorConversion"), "factorConversion"));
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        boolean esBorrable = esBorrableTmp != null ? esBorrableTmp : true;
        String idUnidadOrigen = Validador.textoRequerido(request.get("idUnidadOrigen"), "idUnidadOrigen");
        String idUnidadDestino = Validador.textoRequerido(request.get("idUnidadDestino"), "idUnidadDestino");

        return new CrearConversionRequest(factorConversion, esBorrable, idUnidadOrigen, idUnidadDestino);
    }

    public BigDecimal getFactorConversion() {return factorConversion;}
    public boolean isEsBorrable() {return esBorrable;}
    public String getIdUnidadOrigen() {return idUnidadOrigen;}
    public String getIdUnidadDestino() {return idUnidadDestino;}
}
