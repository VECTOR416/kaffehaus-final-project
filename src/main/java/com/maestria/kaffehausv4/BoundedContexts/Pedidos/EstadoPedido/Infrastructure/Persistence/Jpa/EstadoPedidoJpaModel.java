package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_pedido")
public class EstadoPedidoJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    public EstadoPedidoJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public OffsetDateTime getFecC() {return fecC;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public OffsetDateTime getFecU() {return fecU;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
}
