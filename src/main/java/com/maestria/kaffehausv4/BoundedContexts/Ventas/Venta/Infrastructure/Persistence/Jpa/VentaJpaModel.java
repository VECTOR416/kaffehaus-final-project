package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "venta")
public class VentaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_cliente")
    private UUID idCliente;

    @Column(name = "id_estado_venta", nullable = false)
    private UUID idEstadoVenta;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    public VentaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public void setFechaVenta(LocalDateTime fechaVenta) {this.fechaVenta = fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public UUID getIdUsuario() {return idUsuario;}
    public void setIdUsuario(UUID idUsuario) {this.idUsuario = idUsuario;}
    public UUID getIdCliente() {return idCliente;}
    public void setIdCliente(UUID idCliente) {this.idCliente = idCliente;}
    public UUID getIdEstadoVenta() {return idEstadoVenta;}
    public void setIdEstadoVenta(UUID idEstadoVenta) {this.idEstadoVenta = idEstadoVenta;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
}
