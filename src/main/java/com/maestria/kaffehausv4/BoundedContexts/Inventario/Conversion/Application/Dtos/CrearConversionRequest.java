package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearConversionRequest {
    private BigDecimal factorConversion;
    private boolean esBorrable;
    private String idUnidadOrigen;
    private String idUnidadDestino;
    private boolean borrado;

    private CrearConversionRequest(
        BigDecimal factorConversion,
        boolean esBorrable,
        String idUnidadOrigen,
        String idUnidadDestino,
        boolean borrado
    ) {
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.idUnidadOrigen = idUnidadOrigen;
        this.idUnidadDestino = idUnidadDestino;
        this.borrado = borrado;
    }

    public static CrearConversionRequest createFromRequest(Map<String, Object> request){
        BigDecimal factorConversion = BigDecimal.valueOf(Validador.numeroRequerido(request.get("factorConversion"), "factorConversion"));
        Boolean esBorrableTmp = Validador.booleanoOpcional(request.get("esBorrable"), "esBorrable");
        boolean esBorrable = esBorrableTmp != null ? esBorrableTmp : true;
        String idUnidadOrigen = Validador.textoRequerido(request.get("idUnidadOrigen"), "idUnidadOrigen");
        String idUnidadDestino = Validador.textoRequerido(request.get("idUnidadDestino"), "idUnidadDestino");

        return new CrearConversionRequest(factorConversion, esBorrable, idUnidadOrigen, idUnidadDestino, false);
    }

    public BigDecimal getFactorConversion() {return factorConversion;}
    public void setFactorConversion(BigDecimal factorConversion) {this.factorConversion = factorConversion;}
    public boolean isEsBorrable() {return esBorrable;}
    public void setEsBorrable(boolean esBorrable) {this.esBorrable = esBorrable;}
    public String getIdUnidadOrigen() {return idUnidadOrigen;}
    public void setIdUnidadOrigen(String idUnidadOrigen) {this.idUnidadOrigen = idUnidadOrigen;}
    public String getIdUnidadDestino() {return idUnidadDestino;}
    public void setIdUnidadDestino(String idUnidadDestino) {this.idUnidadDestino = idUnidadDestino;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
