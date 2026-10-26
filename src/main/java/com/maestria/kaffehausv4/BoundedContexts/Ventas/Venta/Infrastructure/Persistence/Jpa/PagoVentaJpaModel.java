package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pago_venta")
public class PagoVentaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_venta", nullable = false)
    private UUID idVenta;

    @Column(name = "id_tipo_pago", nullable = false)
    private UUID idTipoPago;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "monto_recibido", nullable = false)
    private BigDecimal montoRecibido;

    @Column(name = "cambio", nullable = false)
    private BigDecimal cambio;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public PagoVentaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public UUID getIdVenta() {return idVenta;}
    public void setIdVenta(UUID idVenta) {this.idVenta = idVenta;}
    public UUID getIdTipoPago() {return idTipoPago;}
    public void setIdTipoPago(UUID idTipoPago) {this.idTipoPago = idTipoPago;}
    public BigDecimal getMonto() {return monto;}
    public void setMonto(BigDecimal monto) {this.monto = monto;}
    public BigDecimal getMontoRecibido() {return montoRecibido;}
    public void setMontoRecibido(BigDecimal montoRecibido) {this.montoRecibido = montoRecibido;}
    public BigDecimal getCambio() {return cambio;}
    public void setCambio(BigDecimal cambio) {this.cambio = cambio;}
    public String getReferencia() {return referencia;}
    public void setReferencia(String referencia) {this.referencia = referencia;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
