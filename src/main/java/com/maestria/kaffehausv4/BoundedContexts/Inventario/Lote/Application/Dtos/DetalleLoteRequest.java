package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Linea del payload de alta de un lote. No es un request de primer nivel: solo viaja anidada
 * dentro de CrearLoteRequest, porque un detalle no se registra fuera de su lote.
 *
 * No admite 'cantidadRestante': una compra recien registrada no tiene consumo, asi que la
 * raiz la iguala a la cantidad inicial. Tampoco admite el costo de la linea, que es derivado.
 */
public class DetalleLoteRequest {
    private String idProducto;
    private BigDecimal cantidadInicial;
    private BigDecimal precioUnitario;
    private Boolean fraccionable;

    private DetalleLoteRequest(
        String idProducto,
        BigDecimal cantidadInicial,
        BigDecimal precioUnitario,
        Boolean fraccionable
    ) {
        this.idProducto = idProducto;
        this.cantidadInicial = cantidadInicial;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
    }

    public static DetalleLoteRequest createFromRequest(Map<String, Object> request){
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        BigDecimal cantidadInicial = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadInicial"), "cantidadInicial"));
        BigDecimal precioUnitario = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precioUnitario"), "precioUnitario"));
        Boolean fraccionable = Validador.booleanoOpcional(request.get("fraccionable"), "fraccionable");

        return new DetalleLoteRequest(idProducto, cantidadInicial, precioUnitario, fraccionable);
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public Boolean getFraccionable() {return fraccionable;}
}
