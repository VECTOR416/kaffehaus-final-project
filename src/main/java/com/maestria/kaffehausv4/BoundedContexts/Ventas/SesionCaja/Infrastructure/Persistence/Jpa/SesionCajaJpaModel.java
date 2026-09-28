package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sesion_caja")
public class SesionCajaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_apertura", nullable = false)
    private BigDecimal montoApertura;

    @Column(name = "monto_cierre_declarado")
    private BigDecimal montoCierreDeclarado;

    @Column(name = "monto_cierre_esperado")
    private BigDecimal montoCierreEsperado;

    @Column(name = "diferencia")
    private BigDecimal diferencia;

    @Column(name = "motivo_diferencia")
    private String motivoDiferencia;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public SesionCajaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public UUID getIdUsuario() {return idUsuario;}
    public void setIdUsuario(UUID idUsuario) {this.idUsuario = idUsuario;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public void setFechaApertura(LocalDateTime fechaApertura) {this.fechaApertura = fechaApertura;}
    public LocalDateTime getFechaCierre() {return fechaCierre;}
    public void setFechaCierre(LocalDateTime fechaCierre) {this.fechaCierre = fechaCierre;}
    public BigDecimal getMontoApertura() {return montoApertura;}
    public void setMontoApertura(BigDecimal montoApertura) {this.montoApertura = montoApertura;}
    public BigDecimal getMontoCierreDeclarado() {return montoCierreDeclarado;}
    public void setMontoCierreDeclarado(BigDecimal montoCierreDeclarado) {this.montoCierreDeclarado = montoCierreDeclarado;}
    public BigDecimal getMontoCierreEsperado() {return montoCierreEsperado;}
    public void setMontoCierreEsperado(BigDecimal montoCierreEsperado) {this.montoCierreEsperado = montoCierreEsperado;}
    public BigDecimal getDiferencia() {return diferencia;}
    public void setDiferencia(BigDecimal diferencia) {this.diferencia = diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public void setMotivoDiferencia(String motivoDiferencia) {this.motivoDiferencia = motivoDiferencia;}
    public String getEstado() {return estado;}
    public void setEstado(String estado) {this.estado = estado;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
