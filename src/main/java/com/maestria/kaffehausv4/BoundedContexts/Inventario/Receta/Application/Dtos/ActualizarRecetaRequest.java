package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarRecetaRequest {
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;

    private ActualizarRecetaRequest(
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido
    ) {
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
    }

    public static ActualizarRecetaRequest createFromRequest(Map<String, Object> request){
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        String titulo = Validador.textoOpcional(request.get("titulo"), "titulo");
        String idProductoObtenido = Validador.textoOpcional(request.get("idProductoObtenido"), "idProductoObtenido");

        return new ActualizarRecetaRequest(cantidad, titulo, idProductoObtenido);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getTitulo() {return titulo;}
    public void setTitulo(String titulo) {this.titulo = titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public void setIdProductoObtenido(String idProductoObtenido) {this.idProductoObtenido = idProductoObtenido;}
}
