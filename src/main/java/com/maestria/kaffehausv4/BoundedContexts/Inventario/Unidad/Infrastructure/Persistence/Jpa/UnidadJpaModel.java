package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidad")
public class UnidadJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "unidad", nullable = false)
    private String unidad;

    @Column(name = "abreviatura")
    private String abreviatura;

    @Column(name = "es_borrable", nullable = false)
    private Boolean esBorrable;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public UnidadJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public String getUnidad() {return unidad;}
    public void setUnidad(String unidad) {this.unidad = unidad;}
    public String getAbreviatura() {return abreviatura;}
    public void setAbreviatura(String abreviatura) {this.abreviatura = abreviatura;}
    public Boolean getEsBorrable() {return esBorrable;}
    public void setEsBorrable(Boolean esBorrable) {this.esBorrable = esBorrable;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
