package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "producto")
public class ProductoJpaModel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "stock", nullable = false)
    private BigDecimal stock;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo;

    @Column(name = "vendible", nullable = false)
    private Boolean vendible;

    @Column(name = "unidad_actual", nullable = false)
    private UUID unidadActual;

    @Column(name = "id_tipo", nullable = false)
    private UUID idTipo;

    @Column(name = "id_categoria", nullable = false)
    private UUID idCategoria;

    @Column(name = "id_producto_padre")
    private UUID idProductoPadre;

    @Column(name = "fec_c", nullable = false)
    private OffsetDateTime fecC;

    @Column(name = "fec_u")
    private OffsetDateTime fecU;

    @Column(name = "borrado", nullable = false)
    private Boolean borrado;

    public ProductoJpaModel() {}

    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public BigDecimal getStock() {return stock;}
    public void setStock(BigDecimal stock) {this.stock = stock;}
    public BigDecimal getPrecio() {return precio;}
    public void setPrecio(BigDecimal precio) {this.precio = precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public void setStockMinimo(BigDecimal stockMinimo) {this.stockMinimo = stockMinimo;}
    public Boolean getVendible() {return vendible;}
    public void setVendible(Boolean vendible) {this.vendible = vendible;}
    public UUID getUnidadActual() {return unidadActual;}
    public void setUnidadActual(UUID unidadActual) {this.unidadActual = unidadActual;}
    public UUID getIdTipo() {return idTipo;}
    public void setIdTipo(UUID idTipo) {this.idTipo = idTipo;}
    public UUID getIdCategoria() {return idCategoria;}
    public void setIdCategoria(UUID idCategoria) {this.idCategoria = idCategoria;}
    public UUID getIdProductoPadre() {return idProductoPadre;}
    public void setIdProductoPadre(UUID idProductoPadre) {this.idProductoPadre = idProductoPadre;}
    public void setFecC(OffsetDateTime fecC) {this.fecC = fecC;}
    public void setFecU(OffsetDateTime fecU) {this.fecU = fecU;}
    public Boolean getBorrado() {return borrado;}
    public void setBorrado(Boolean borrado) {this.borrado = borrado;}
}
