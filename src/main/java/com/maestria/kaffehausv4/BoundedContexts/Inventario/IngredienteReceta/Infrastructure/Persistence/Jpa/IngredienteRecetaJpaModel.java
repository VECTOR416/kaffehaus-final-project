package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredientes_receta")
public class IngredienteRecetaJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "id_receta", nullable = false)
    private UUID idReceta;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public IngredienteRecetaJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public UUID getIdReceta() {return idReceta;}
    public void setIdReceta(UUID idReceta) {this.idReceta = idReceta;}
    public UUID getIdProducto() {return idProducto;}
    public void setIdProducto(UUID idProducto) {this.idProducto = idProducto;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
