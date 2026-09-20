package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class CategoriaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "es_borrable", nullable = false)
    private Boolean esBorrable;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public CategoriaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public Boolean getEsBorrable() {return esBorrable;}
    public void setEsBorrable(Boolean esBorrable) {this.esBorrable = esBorrable;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
