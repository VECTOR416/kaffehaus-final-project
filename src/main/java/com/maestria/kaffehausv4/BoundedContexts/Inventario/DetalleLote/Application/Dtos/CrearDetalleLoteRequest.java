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

    private CrearDetalleLoteRequest(
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
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

    public static CrearDetalleLoteRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidadInicial = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadInicial"), "cantidadInicial"));
        BigDecimal cantidadRestante = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadRestante"), "cantidadRestante"));
        BigDecimal precioUnitario = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precioUnitario"), "precioUnitario"));
        Boolean fraccionableTmp = Validador.booleanoOpcional(request.get("fraccionable"), "fraccionable");
        boolean fraccionable = fraccionableTmp != null ? fraccionableTmp : false;
        String idLote = Validador.textoRequerido(request.get("idLote"), "idLote");
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");

        return new CrearDetalleLoteRequest(cantidadInicial, cantidadRestante, precioUnitario, fraccionable, idLote, idProducto);
    }

    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public boolean isFraccionable() {return fraccionable;}
    public String getIdLote() {return idLote;}
    public String getIdProducto() {return idProducto;}
}
