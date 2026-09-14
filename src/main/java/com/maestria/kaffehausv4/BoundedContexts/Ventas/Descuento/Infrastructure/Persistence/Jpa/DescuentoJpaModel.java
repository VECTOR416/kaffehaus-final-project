package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "descuentos")
public class DescuentoJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "descuento", nullable = false)
    private BigDecimal descuento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_final", nullable = false)
    private LocalDateTime fechaFinal;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "id_tipo_descuento", nullable = false)
    private UUID idTipoDescuento;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public DescuentoJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public void setFechaInicio(LocalDateTime fechaInicio) {this.fechaInicio = fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public void setFechaFinal(LocalDateTime fechaFinal) {this.fechaFinal = fechaFinal;}
    public Boolean getEstado() {return estado;}
    public void setEstado(Boolean estado) {this.estado = estado;}
    public UUID getIdProducto() {return idProducto;}
    public void setIdProducto(UUID idProducto) {this.idProducto = idProducto;}
    public UUID getIdTipoDescuento() {return idTipoDescuento;}
    public void setIdTipoDescuento(UUID idTipoDescuento) {this.idTipoDescuento = idTipoDescuento;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
