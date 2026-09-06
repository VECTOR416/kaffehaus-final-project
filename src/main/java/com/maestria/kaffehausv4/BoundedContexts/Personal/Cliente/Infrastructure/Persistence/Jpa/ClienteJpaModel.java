package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class ClienteJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_persona", nullable = false)
    private UUID idPersona;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public ClienteJpaModel() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(UUID idPersona) {
        this.idPersona = idPersona;
    }

    public OffsetDateTime getFecC() {
        return fecC;
    }

    public void setFecC(OffsetDateTime fecC) {
        this.fecC = fecC;
    }

    public OffsetDateTime getFecU() {
        return fecU;
    }

    public void setFecU(OffsetDateTime fecU) {
        this.fecU = fecU;
    }

    public Boolean getBorrado() {
        return borrado;
    }

    public void setBorrado(Boolean borrado) {
        this.borrado = borrado;
    }
}
