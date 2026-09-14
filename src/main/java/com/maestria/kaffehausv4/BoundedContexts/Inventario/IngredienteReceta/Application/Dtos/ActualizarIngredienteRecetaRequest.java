package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarIngredienteRecetaRequest {
    private BigDecimal cantidad;
    private String idReceta;
    private String idProducto;

    private ActualizarIngredienteRecetaRequest(
        BigDecimal cantidad,
        String idReceta,
        String idProducto
    ) {
        this.cantidad = cantidad;
        this.idReceta = idReceta;
        this.idProducto = idProducto;
    }

    public static ActualizarIngredienteRecetaRequest createFromRequest(Map<String, Object> request){
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        String idReceta = Validador.textoOpcional(request.get("idReceta"), "idReceta");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");

        return new ActualizarIngredienteRecetaRequest(cantidad, idReceta, idProducto);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getIdReceta() {return idReceta;}
    public String getIdProducto() {return idProducto;}
}
