package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class Produccion {
    private ProduccionId id;
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private RecetaId recetaId;
    private boolean borrado;

    private Produccion(
        ProduccionId id,
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId,
        boolean borrado
    ){
        this.id = id;
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.recetaId = recetaId;
        this.borrado = borrado;
    }

    public static Produccion restaurar(
        String id,
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId,
        boolean borrado
    ){
        return new Produccion(new ProduccionId(id), cantidadEsperada, cantidadReal, diferencia, motivoDiferencia, fechaProduccion, recetaId, borrado);
    }

    public static Produccion crear(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId
    ){
        return new Produccion(null, cantidadEsperada, cantidadReal, diferencia, motivoDiferencia, fechaProduccion, recetaId, false);
    }

    public void actualizar(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId
    ){
        this.cantidadEsperada = cantidadEsperada != null ? cantidadEsperada : this.cantidadEsperada;
        this.cantidadReal = cantidadReal != null ? cantidadReal : this.cantidadReal;
        this.diferencia = diferencia != null ? diferencia : this.diferencia;
        this.motivoDiferencia = motivoDiferencia != null ? motivoDiferencia : this.motivoDiferencia;
        this.fechaProduccion = fechaProduccion != null ? fechaProduccion : this.fechaProduccion;
        this.recetaId = recetaId != null ? recetaId : this.recetaId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public ProduccionId getId() {return id;}
    public BigDecimal getCantidadEsperada() {return cantidadEsperada;}
    public BigDecimal getCantidadReal() {return cantidadReal;}
    public BigDecimal getDiferencia() {return diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public LocalDateTime getFechaProduccion() {return fechaProduccion;}
    public RecetaId getRecetaId() {return recetaId;}
    public boolean isBorrado() {return borrado;}
}
