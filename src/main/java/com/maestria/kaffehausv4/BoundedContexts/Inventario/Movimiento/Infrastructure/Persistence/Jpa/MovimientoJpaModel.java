package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimientos")
public class MovimientoJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "id_existencia", nullable = false)
    private UUID idExistencia;

    @Column(name = "id_tipo_movimiento", nullable = false)
    private UUID idTipoMovimiento;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public MovimientoJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {this.fechaMovimiento = fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
    public UUID getIdExistencia() {return idExistencia;}
    public void setIdExistencia(UUID idExistencia) {this.idExistencia = idExistencia;}
    public UUID getIdTipoMovimiento() {return idTipoMovimiento;}
    public void setIdTipoMovimiento(UUID idTipoMovimiento) {this.idTipoMovimiento = idTipoMovimiento;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
