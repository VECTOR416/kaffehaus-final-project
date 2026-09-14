package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lote")
public class LoteJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "id_proveedor", nullable = false)
    private UUID idProveedor;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public LoteJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public void setFechaCompra(LocalDateTime fechaCompra) {this.fechaCompra = fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public UUID getIdProveedor() {return idProveedor;}
    public void setIdProveedor(UUID idProveedor) {this.idProveedor = idProveedor;}
    public UUID getIdUsuario() {return idUsuario;}
    public void setIdUsuario(UUID idUsuario) {this.idUsuario = idUsuario;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
