package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearIngredienteRecetaRequest {
    private BigDecimal cantidad;
    private String idReceta;
    private String idProducto;
    private boolean borrado;

    private CrearIngredienteRecetaRequest(
        BigDecimal cantidad,
        String idReceta,
        String idProducto,
        boolean borrado
    ) {
        this.cantidad = cantidad;
        this.idReceta = idReceta;
        this.idProducto = idProducto;
        this.borrado = borrado;
    }

    public static CrearIngredienteRecetaRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");

        return new CrearIngredienteRecetaRequest(cantidad, idReceta, idProducto, false);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getIdReceta() {return idReceta;}
    public void setIdReceta(String idReceta) {this.idReceta = idReceta;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
