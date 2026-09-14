package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearIngredienteRecetaRequest {
    private BigDecimal cantidad;
    private String idReceta;
    private String idProducto;

    private CrearIngredienteRecetaRequest(
        BigDecimal cantidad,
        String idReceta,
        String idProducto
    ) {
        this.cantidad = cantidad;
        this.idReceta = idReceta;
        this.idProducto = idProducto;
    }

    public static CrearIngredienteRecetaRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");

        return new CrearIngredienteRecetaRequest(cantidad, idReceta, idProducto);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getIdReceta() {return idReceta;}
    public String getIdProducto() {return idProducto;}
}
