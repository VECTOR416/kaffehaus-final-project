package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class Conversion {
    private ConversionId id;
    private BigDecimal factorConversion;
    private boolean esBorrable;
    private UnidadId unidadOrigenId;
    private UnidadId unidadDestinoId;
    private boolean borrado;

    private Conversion(
        ConversionId id,
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId,
        boolean borrado
    ){
        this.id = id;
        this.factorConversion = factorConversion;
        this.esBorrable = esBorrable;
        this.unidadOrigenId = unidadOrigenId;
        this.unidadDestinoId = unidadDestinoId;
        this.borrado = borrado;
    }

    public static Conversion restaurar(
        String id,
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId,
        boolean borrado
    ){
        return new Conversion(new ConversionId(id), factorConversion, esBorrable, unidadOrigenId, unidadDestinoId, borrado);
    }

    public static Conversion crear(
        BigDecimal factorConversion,
        boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId
    ){
        return new Conversion(null, factorConversion, esBorrable, unidadOrigenId, unidadDestinoId, false);
    }

    public void actualizar(
        BigDecimal factorConversion,
        Boolean esBorrable,
        UnidadId unidadOrigenId,
        UnidadId unidadDestinoId
    ){
        this.factorConversion = factorConversion != null ? factorConversion : this.factorConversion;
        this.esBorrable = esBorrable != null ? esBorrable : this.esBorrable;
        this.unidadOrigenId = unidadOrigenId != null ? unidadOrigenId : this.unidadOrigenId;
        this.unidadDestinoId = unidadDestinoId != null ? unidadDestinoId : this.unidadDestinoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public ConversionId getId() {return id;}
    public BigDecimal getFactorConversion() {return factorConversion;}
    public boolean isEsBorrable() {return esBorrable;}
    public UnidadId getUnidadOrigenId() {return unidadOrigenId;}
    public UnidadId getUnidadDestinoId() {return unidadDestinoId;}
    public boolean isBorrado() {return borrado;}
}
