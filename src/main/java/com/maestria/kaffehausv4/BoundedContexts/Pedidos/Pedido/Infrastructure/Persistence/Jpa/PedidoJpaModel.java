package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")
public class PedidoJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "latitud", nullable = false)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false)
    private BigDecimal longitud;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "id_venta", nullable = false)
    private UUID idVenta;

    @Column(name = "id_estado", nullable = false)
    private UUID idEstado;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    public PedidoJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public void setFechaEntrega(LocalDateTime fechaEntrega) {this.fechaEntrega = fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public void setLatitud(BigDecimal latitud) {this.latitud = latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public void setLongitud(BigDecimal longitud) {this.longitud = longitud;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public UUID getIdVenta() {return idVenta;}
    public void setIdVenta(UUID idVenta) {this.idVenta = idVenta;}
    public UUID getIdEstado() {return idEstado;}
    public void setIdEstado(UUID idEstado) {this.idEstado = idEstado;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
}
