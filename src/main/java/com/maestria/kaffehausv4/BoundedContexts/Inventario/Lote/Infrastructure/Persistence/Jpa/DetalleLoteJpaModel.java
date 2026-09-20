package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_lote")
public class DetalleLoteJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "cantidad_inicial", nullable = false)
    private BigDecimal cantidadInicial;

    @Column(name = "cantidad_restante", nullable = false)
    private BigDecimal cantidadRestante;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "fraccionable", nullable = false)
    private Boolean fraccionable;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "id_lote", nullable = false)
    private UUID idLote;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public DetalleLoteJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public void setCantidadInicial(BigDecimal cantidadInicial) {this.cantidadInicial = cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public void setCantidadRestante(BigDecimal cantidadRestante) {this.cantidadRestante = cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public Boolean getFraccionable() {return fraccionable;}
    public void setFraccionable(Boolean fraccionable) {this.fraccionable = fraccionable;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public void setFechaVencimiento(LocalDate fechaVencimiento) {this.fechaVencimiento = fechaVencimiento;}
    public UUID getIdLote() {return idLote;}
    public void setIdLote(UUID idLote) {this.idLote = idLote;}
    public UUID getIdProducto() {return idProducto;}
    public void setIdProducto(UUID idProducto) {this.idProducto = idProducto;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
