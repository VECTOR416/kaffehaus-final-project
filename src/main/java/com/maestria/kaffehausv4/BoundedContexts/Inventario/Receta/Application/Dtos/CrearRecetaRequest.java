package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearRecetaRequest {
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;
    private boolean borrado;

    private CrearRecetaRequest(
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido,
        boolean borrado
    ) {
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
        this.borrado = borrado;
    }

    public static CrearRecetaRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String titulo = Validador.textoRequerido(request.get("titulo"), "titulo");
        String idProductoObtenido = Validador.textoRequerido(request.get("idProductoObtenido"), "idProductoObtenido");

        return new CrearRecetaRequest(cantidad, titulo, idProductoObtenido, false);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getTitulo() {return titulo;}
    public void setTitulo(String titulo) {this.titulo = titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public void setIdProductoObtenido(String idProductoObtenido) {this.idProductoObtenido = idProductoObtenido;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
