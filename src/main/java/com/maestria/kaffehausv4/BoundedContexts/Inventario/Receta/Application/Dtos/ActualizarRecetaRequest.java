package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Modificacion de una receta existente. Mantiene la semantica PATCH del proyecto: lo que no
 * llega, no se toca.
 *
 * 'ingredientes' y 'pasos' son opcionales e independientes entre si: se puede tocar una
 * coleccion sin mencionar la otra. Cuando una viene, el caso de uso la concilia por id: el
 * elemento con 'id' se corrige, el que no lo trae se agrega y el ausente se da de baja logica.
 * No se reemplaza la coleccion entera porque el sistema no usa borrado fisico.
 *
 * En 'pasos' el orden del array es ademas la secuencia de ejecucion resultante.
 */
public class ActualizarRecetaRequest {
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;
    private List<ActualizarIngredienteRecetaRequest> ingredientes;
    private List<ActualizarPasoRecetaRequest> pasos;

    private ActualizarRecetaRequest(
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido,
        List<ActualizarIngredienteRecetaRequest> ingredientes,
        List<ActualizarPasoRecetaRequest> pasos
    ) {
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
        this.ingredientes = ingredientes;
        this.pasos = pasos;
    }

    public static ActualizarRecetaRequest createFromRequest(Map<String, Object> request){
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        String titulo = Validador.textoOpcional(request.get("titulo"), "titulo");
        String idProductoObtenido = Validador.textoOpcional(request.get("idProductoObtenido"), "idProductoObtenido");

        List<Map<String, Object>> ingredientesTmp = Validador.listaDeObjetosOpcional(request.get("ingredientes"), "ingredientes");
        List<ActualizarIngredienteRecetaRequest> ingredientes = ingredientesTmp != null
                ? ingredientesTmp.stream().map(ActualizarIngredienteRecetaRequest::createFromRequest).collect(Collectors.toList())
                : null;

        List<Map<String, Object>> pasosTmp = Validador.listaDeObjetosOpcional(request.get("pasos"), "pasos");
        List<ActualizarPasoRecetaRequest> pasos = pasosTmp != null
                ? pasosTmp.stream().map(ActualizarPasoRecetaRequest::createFromRequest).collect(Collectors.toList())
                : null;

        return new ActualizarRecetaRequest(cantidad, titulo, idProductoObtenido, ingredientes, pasos);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public List<ActualizarIngredienteRecetaRequest> getIngredientes() {return ingredientes;}
    public List<ActualizarPasoRecetaRequest> getPasos() {return pasos;}
}
