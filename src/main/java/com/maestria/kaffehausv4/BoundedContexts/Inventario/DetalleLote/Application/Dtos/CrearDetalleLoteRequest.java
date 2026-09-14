package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearDetalleLoteRequest {
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private boolean fraccionable;
    private String idLote;
    private String idProducto;
    private boolean borrado;

    private CrearDetalleLoteRequest(
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        String idLote,
        String idProducto,
        boolean borrado
    ) {
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.idLote = idLote;
        this.idProducto = idProducto;
        this.borrado = borrado;
    }

    public static CrearDetalleLoteRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidadInicial = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadInicial"), "cantidadInicial"));
        BigDecimal cantidadRestante = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadRestante"), "cantidadRestante"));
        BigDecimal precioUnitario = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precioUnitario"), "precioUnitario"));
        Boolean fraccionableTmp = Validador.booleanoOpcional(request.get("fraccionable"), "fraccionable");
        boolean fraccionable = fraccionableTmp != null ? fraccionableTmp : false;
        String idLote = Validador.textoRequerido(request.get("idLote"), "idLote");
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");

        return new CrearDetalleLoteRequest(cantidadInicial, cantidadRestante, precioUnitario, fraccionable, idLote, idProducto, false);
    }

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
