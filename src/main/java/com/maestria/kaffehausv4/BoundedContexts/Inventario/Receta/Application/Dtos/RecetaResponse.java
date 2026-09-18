package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;

/**
 * Respuesta del agregado completo: encabezado, ingredientes y pasos vivos. Los pasos llegan
 * ordenados por su numero de secuencia; lo dado de baja no se devuelve.
 */
public class RecetaResponse {
    private String id;
    private BigDecimal cantidad;
    private String titulo;
    private String idProductoObtenido;
    private boolean borrado;
    private List<IngredienteRecetaResponse> ingredientes;
    private List<PasoRecetaResponse> pasos;

    private RecetaResponse(
        String id,
        BigDecimal cantidad,
        String titulo,
        String idProductoObtenido,
        boolean borrado,
        List<IngredienteRecetaResponse> ingredientes,
        List<PasoRecetaResponse> pasos
    ) {
        this.id = id;
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.idProductoObtenido = idProductoObtenido;
        this.borrado = borrado;
        this.ingredientes = ingredientes;
        this.pasos = pasos;
    }

    public static RecetaResponse createFromAggregate(Receta receta){
        List<IngredienteRecetaResponse> ingredientes = receta.getIngredientesActivos().stream()
                .map(IngredienteRecetaResponse::createFromAggregate)
                .collect(Collectors.toList());

        List<PasoRecetaResponse> pasos = receta.getPasosActivos().stream()
                .map(PasoRecetaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new RecetaResponse(
            receta.getId().getValue(),
            receta.getCantidad(),
            receta.getTitulo(),
            receta.getProductoObtenidoId() != null ? receta.getProductoObtenidoId().getValue() : null,
            receta.isBorrado(),
            ingredientes,
            pasos
        );
    }

    public String getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public String getIdProductoObtenido() {return idProductoObtenido;}
    public boolean isBorrado() {return borrado;}
    public List<IngredienteRecetaResponse> getIngredientes() {return ingredientes;}
    public List<PasoRecetaResponse> getPasos() {return pasos;}
}
