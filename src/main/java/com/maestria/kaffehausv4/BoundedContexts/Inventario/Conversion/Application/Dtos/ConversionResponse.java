package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

public class ConversionResponse {
    private String id;
    private BigDecimal factorConversion;
    private boolean esBorrable;
    private UnidadResponse unidadOrigen;
    private UnidadResponse unidadDestino;
    private boolean borrado;

    private ConversionResponse(
        String id,
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadResponse unidadOrigen,
        UnidadResponse unidadDestino,
        boolean borrado
    ) {
        this.id = id;
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.unidadOrigen = unidadOrigen;
        this.unidadDestino = unidadDestino;
        this.borrado = borrado;
    }

    public static ConversionResponse createFromAggregate(Conversion conversion, Unidad unidadOrigen, Unidad unidadDestino){
        UnidadResponse unidadOrigenResponse = unidadOrigen != null ? UnidadResponse.createFromAggregate(unidadOrigen) : null;
        UnidadResponse unidadDestinoResponse = unidadDestino != null ? UnidadResponse.createFromAggregate(unidadDestino) : null;
        return new ConversionResponse(
            conversion.getId().getValue(),
            conversion.getFactorConversion(),
            conversion.isEsBorrable(),
            unidadOrigenResponse,
            unidadDestinoResponse,
            conversion.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getFactorConversion() {return factorConversion;}
    public boolean isEsBorrable() {return esBorrable;}
    public UnidadResponse getUnidadOrigen() {return unidadOrigen;}
    public UnidadResponse getUnidadDestino() {return unidadDestino;}
    public boolean isBorrado() {return borrado;}
}
