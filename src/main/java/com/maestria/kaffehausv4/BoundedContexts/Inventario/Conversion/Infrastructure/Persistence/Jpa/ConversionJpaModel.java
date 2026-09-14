package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversiones")
public class ConversionJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "factor_conversion", nullable = false)
    private BigDecimal factorConversion;

    @Column(name = "es_borrable", nullable = false)
    private Boolean esBorrable;

    @Column(name = "id_unidad_origen", nullable = false)
    private UUID idUnidadOrigen;

    @Column(name = "id_unidad_destino", nullable = false)
    private UUID idUnidadDestino;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public ConversionJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getFactorConversion() {return factorConversion;}
    public void setFactorConversion(BigDecimal factorConversion) {this.factorConversion = factorConversion;}
    public Boolean getEsBorrable() {return esBorrable;}
    public void setEsBorrable(Boolean esBorrable) {this.esBorrable = esBorrable;}
    public UUID getIdUnidadOrigen() {return idUnidadOrigen;}
    public void setIdUnidadOrigen(UUID idUnidadOrigen) {this.idUnidadOrigen = idUnidadOrigen;}
    public UUID getIdUnidadDestino() {return idUnidadDestino;}
    public void setIdUnidadDestino(UUID idUnidadDestino) {this.idUnidadDestino = idUnidadDestino;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
