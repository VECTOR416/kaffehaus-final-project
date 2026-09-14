package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_venta")
public class DetalleVentaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento_aplicado", nullable = false)
    private BigDecimal descuentoAplicado;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    @Column(name = "id_venta", nullable = false)
    private UUID idVenta;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    public DetalleVentaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public void setSubtotal(BigDecimal subtotal) {this.subtotal = subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public void setDescuentoAplicado(BigDecimal descuentoAplicado) {this.descuentoAplicado = descuentoAplicado;}
    public Boolean getEstado() {return estado;}
    public void setEstado(Boolean estado) {this.estado = estado;}
    public UUID getIdVenta() {return idVenta;}
    public void setIdVenta(UUID idVenta) {this.idVenta = idVenta;}
    public UUID getIdProducto() {return idProducto;}
    public void setIdProducto(UUID idProducto) {this.idProducto = idProducto;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
}
