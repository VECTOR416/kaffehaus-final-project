package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Payload compuesto del alta de una receta: encabezado, ingredientes y pasos en una sola
 * peticion. Ambas colecciones son obligatorias y no pueden venir vacias: una receta dice con
 * que se hace y como se hace.
 *
 * El orden de 'pasos' en el array es la secuencia de ejecucion; no se envia el numero de paso.
 */
public class CrearRecetaRequest {
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;
    private List<IngredienteRecetaRequest> ingredientes;
    private List<PasoRecetaRequest> pasos;

    private CrearRecetaRequest(
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido,
        List<IngredienteRecetaRequest> ingredientes,
        List<PasoRecetaRequest> pasos
    ) {
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
        this.ingredientes = ingredientes;
        this.pasos = pasos;
    }

    public static CrearRecetaRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        String titulo = Validador.textoRequerido(request.get("titulo"), "titulo");
        String idProductoObtenido = Validador.textoRequerido(request.get("idProductoObtenido"), "idProductoObtenido");

        List<IngredienteRecetaRequest> ingredientes = Validador.listaDeObjetosRequerida(request.get("ingredientes"), "ingredientes").stream()
                .map(IngredienteRecetaRequest::createFromRequest)
                .collect(Collectors.toList());

        List<PasoRecetaRequest> pasos = Validador.listaDeObjetosRequerida(request.get("pasos"), "pasos").stream()
                .map(PasoRecetaRequest::createFromRequest)
                .collect(Collectors.toList());

        return new CrearRecetaRequest(cantidad, titulo, idProductoObtenido, ingredientes, pasos);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public List<IngredienteRecetaRequest> getIngredientes() {return ingredientes;}
    public List<PasoRecetaRequest> getPasos() {return pasos;}
}
