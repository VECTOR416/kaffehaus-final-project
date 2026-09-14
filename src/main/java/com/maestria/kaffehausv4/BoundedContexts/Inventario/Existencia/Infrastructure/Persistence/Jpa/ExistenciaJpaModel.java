package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "existencias")
public class ExistenciaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "contenido_inicial", nullable = false)
    private BigDecimal contenidoInicial;

    @Column(name = "contenido_actual", nullable = false)
    private BigDecimal contenidoActual;

    @Column(name = "fecha_acabado")
    private LocalDateTime fechaAcabado;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "id_detalle_lote", nullable = false)
    private UUID idDetalleLote;

    @Column(name = "id_estado_existencia", nullable = false)
    private UUID idEstadoExistencia;

    @Column(name = "unidad", nullable = false)
    private UUID unidad;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public ExistenciaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public void setContenidoInicial(BigDecimal contenidoInicial) {this.contenidoInicial = contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public void setContenidoActual(BigDecimal contenidoActual) {this.contenidoActual = contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public void setFechaAcabado(LocalDateTime fechaAcabado) {this.fechaAcabado = fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public void setFechaVencimiento(LocalDate fechaVencimiento) {this.fechaVencimiento = fechaVencimiento;}
    public UUID getIdDetalleLote() {return idDetalleLote;}
    public void setIdDetalleLote(UUID idDetalleLote) {this.idDetalleLote = idDetalleLote;}
    public UUID getIdEstadoExistencia() {return idEstadoExistencia;}
    public void setIdEstadoExistencia(UUID idEstadoExistencia) {this.idEstadoExistencia = idEstadoExistencia;}
    public UUID getUnidad() {return unidad;}
    public void setUnidad(UUID unidad) {this.unidad = unidad;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
