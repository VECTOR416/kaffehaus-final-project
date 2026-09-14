package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearRecetaRequest {
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;

    private CrearRecetaRequest(
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido
    ) {
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
    }

    public static CrearRecetaRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String titulo = Validador.textoRequerido(request.get("titulo"), "titulo");
        String idProductoObtenido = Validador.textoRequerido(request.get("idProductoObtenido"), "idProductoObtenido");

        return new CrearRecetaRequest(cantidad, titulo, idProductoObtenido);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
}
