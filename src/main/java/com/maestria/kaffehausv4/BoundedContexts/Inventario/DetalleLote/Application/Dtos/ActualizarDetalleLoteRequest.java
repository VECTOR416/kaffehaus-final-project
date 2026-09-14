package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarDetalleLoteRequest {
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private Boolean fraccionable;
    private String idLote;
    private String idProducto;

    private ActualizarDetalleLoteRequest(
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        Boolean fraccionable,
        String idLote,
        String idProducto
    ) {
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.idLote = idLote;
        this.idProducto = idProducto;
    }

    public static ActualizarDetalleLoteRequest createFromRequest(Map<String, Object> request){
        Double cantidadInicialTmp = Validador.numeroOpcional(request.get("cantidadInicial"), "cantidadInicial");
        BigDecimal cantidadInicial = cantidadInicialTmp != null ? BigDecimal.valueOf(cantidadInicialTmp) : null;
        Double cantidadRestanteTmp = Validador.numeroOpcional(request.get("cantidadRestante"), "cantidadRestante");
        BigDecimal cantidadRestante = cantidadRestanteTmp != null ? BigDecimal.valueOf(cantidadRestanteTmp) : null;
        Double precioUnitarioTmp = Validador.numeroOpcional(request.get("precioUnitario"), "precioUnitario");
        BigDecimal precioUnitario = precioUnitarioTmp != null ? BigDecimal.valueOf(precioUnitarioTmp) : null;
        Boolean fraccionableTmp = Validador.booleanoOpcional(request.get("fraccionable"), "fraccionable");
        Boolean fraccionable = fraccionableTmp;
        String idLote = Validador.textoOpcional(request.get("idLote"), "idLote");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");

        return new ActualizarDetalleLoteRequest(cantidadInicial, cantidadRestante, precioUnitario, fraccionable, idLote, idProducto);
    }

    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public void setCantidadInicial(BigDecimal cantidadInicial) {this.cantidadInicial = cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public void setCantidadRestante(BigDecimal cantidadRestante) {this.cantidadRestante = cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public Boolean getFraccionable() {return fraccionable;}
    public void setFraccionable(Boolean fraccionable) {this.fraccionable = fraccionable;}
    public String getIdLote() {return idLote;}
    public void setIdLote(String idLote) {this.idLote = idLote;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
}
