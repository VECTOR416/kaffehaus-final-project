package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "produccion")
public class ProduccionJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "cantidad_esperada", nullable = false)
    private BigDecimal cantidadEsperada;

    @Column(name = "cantidad_real", nullable = false)
    private BigDecimal cantidadReal;

    @Column(name = "diferencia", nullable = false)
    private BigDecimal diferencia;

    @Column(name = "motivo_diferencia")
    private String motivoDiferencia;

    @Column(name = "fecha_produccion", nullable = false)
    private LocalDateTime fechaProduccion;

    @Column(name = "id_receta", nullable = false)
    private UUID idReceta;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public ProduccionJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
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
    public UUID getIdReceta() {return idReceta;}
    public void setIdReceta(UUID idReceta) {this.idReceta = idReceta;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
