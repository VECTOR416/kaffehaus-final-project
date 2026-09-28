package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de movimiento_caja. A diferencia del resto de modelos expone fec_c: la tabla no tiene
 * otra fecha y es el instante del movimiento que se muestra en el arqueo.
 */
@Entity
@Table(name = "movimiento_caja")
public class MovimientoCajaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_sesion_caja", nullable = false)
    private UUID idSesionCaja;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public MovimientoCajaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public UUID getIdSesionCaja() {return idSesionCaja;}
    public void setIdSesionCaja(UUID idSesionCaja) {this.idSesionCaja = idSesionCaja;}
    public UUID getIdUsuario() {return idUsuario;}
    public void setIdUsuario(UUID idUsuario) {this.idUsuario = idUsuario;}
    public String getTipo() {return tipo;}
    public void setTipo(String tipo) {this.tipo = tipo;}
    public BigDecimal getMonto() {return monto;}
    public void setMonto(BigDecimal monto) {this.monto = monto;}
    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
