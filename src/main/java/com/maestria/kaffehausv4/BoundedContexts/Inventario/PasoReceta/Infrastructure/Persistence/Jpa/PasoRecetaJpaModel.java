package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pasos_receta")
public class PasoRecetaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "pasos", nullable = false)
    private Integer pasos;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "id_receta", nullable = false)
    private UUID idReceta;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public PasoRecetaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public Integer getPasos() {return pasos;}
    public void setPasos(Integer pasos) {this.pasos = pasos;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public UUID getIdReceta() {return idReceta;}
    public void setIdReceta(UUID idReceta) {this.idReceta = idReceta;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
