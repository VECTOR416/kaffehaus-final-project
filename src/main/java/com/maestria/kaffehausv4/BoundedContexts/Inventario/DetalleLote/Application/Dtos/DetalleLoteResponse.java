package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;

public class DetalleLoteResponse {
    private String id;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private boolean fraccionable;
    private String idLote;
    private String idProducto;
    private boolean borrado;

    private DetalleLoteResponse(
        String id,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        String idLote,
        String idProducto,
        boolean borrado
    ) {
        this.id = id;
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.idLote = idLote;
        this.idProducto = idProducto;
        this.borrado = borrado;
    }

    public static DetalleLoteResponse createFromAggregate(DetalleLote detalleLote){
        return new DetalleLoteResponse(
            detalleLote.getId().getValue(),
            detalleLote.getCantidadInicial(),
            detalleLote.getCantidadRestante(),
            detalleLote.getPrecioUnitario(),
            detalleLote.isFraccionable(),
            detalleLote.getLoteId() != null ? detalleLote.getLoteId().getValue() : null,
            detalleLote.getProductoId() != null ? detalleLote.getProductoId().getValue() : null,
            detalleLote.isBorrado()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public void setCantidadInicial(BigDecimal cantidadInicial) {this.cantidadInicial = cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public void setCantidadRestante(BigDecimal cantidadRestante) {this.cantidadRestante = cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public boolean isFraccionable() {return fraccionable;}
    public void setFraccionable(boolean fraccionable) {this.fraccionable = fraccionable;}
    public String getIdLote() {return idLote;}
    public void setIdLote(String idLote) {this.idLote = idLote;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
