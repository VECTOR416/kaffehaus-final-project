package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Ingrediente del payload de alta de una receta. No es un request de primer nivel: solo viaja
 * anidado dentro de CrearRecetaRequest, porque un ingrediente no se registra fuera de su receta.
 */
public class IngredienteRecetaRequest {
    private String idProducto;
    private BigDecimal cantidad;

    private IngredienteRecetaRequest(
        String idProducto,
        BigDecimal cantidad
    ) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public static IngredienteRecetaRequest createFromRequest(Map<String, Object> request){
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));

        return new IngredienteRecetaRequest(idProducto, cantidad);
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
}
