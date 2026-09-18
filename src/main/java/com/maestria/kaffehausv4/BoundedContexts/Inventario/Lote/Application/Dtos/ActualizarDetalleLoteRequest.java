package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Linea del payload de modificacion de un lote. Viaja anidada dentro de ActualizarLoteRequest.
 *
 * El campo 'id' decide la operacion: si viene, la linea existente se corrige; si no, se
 * incorpora una nueva y entonces 'idProducto', 'cantidadInicial' y 'precioUnitario' pasan a
 * ser obligatorios (lo exige el caso de uso, que es quien conoce esa bifurcacion).
 *
 * 'cantidadRestante' solo se admite aqui, nunca en el alta: es un ajuste de inventario
 * (consumo, merma o correccion) y la raiz lo valida contra la cantidad comprada.
 */
public class ActualizarDetalleLoteRequest {
    private String id;
    private String idProducto;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private Boolean fraccionable;

    private ActualizarDetalleLoteRequest(
        String id,
        String idProducto,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        Boolean fraccionable
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
    }

    public static ActualizarDetalleLoteRequest createFromRequest(Map<String, Object> request){
        String id = Validador.textoOpcional(request.get("id"), "id");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");
        Double cantidadInicialTmp = Validador.numeroOpcional(request.get("cantidadInicial"), "cantidadInicial");
        BigDecimal cantidadInicial = cantidadInicialTmp != null ? BigDecimal.valueOf(cantidadInicialTmp) : null;
        Double cantidadRestanteTmp = Validador.numeroOpcional(request.get("cantidadRestante"), "cantidadRestante");
        BigDecimal cantidadRestante = cantidadRestanteTmp != null ? BigDecimal.valueOf(cantidadRestanteTmp) : null;
        Double precioUnitarioTmp = Validador.numeroOpcional(request.get("precioUnitario"), "precioUnitario");
        BigDecimal precioUnitario = precioUnitarioTmp != null ? BigDecimal.valueOf(precioUnitarioTmp) : null;
        Boolean fraccionable = Validador.booleanoOpcional(request.get("fraccionable"), "fraccionable");

        return new ActualizarDetalleLoteRequest(id, idProducto, cantidadInicial, cantidadRestante, precioUnitario, fraccionable);
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public Boolean getFraccionable() {return fraccionable;}
}
