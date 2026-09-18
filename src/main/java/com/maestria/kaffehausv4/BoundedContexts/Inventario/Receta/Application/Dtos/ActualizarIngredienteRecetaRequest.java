package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Ingrediente del payload de modificacion de una receta. Viaja anidado dentro de
 * ActualizarRecetaRequest.
 *
 * El campo 'id' decide la operacion: si viene, el ingrediente existente se corrige; si no, se
 * incorpora uno nuevo y entonces 'idProducto' y 'cantidad' pasan a ser obligatorios (lo exige
 * el caso de uso, que es quien conoce esa bifurcacion).
 */
public class ActualizarIngredienteRecetaRequest {
    private String id;
    private String idProducto;
    private BigDecimal cantidad;

    private ActualizarIngredienteRecetaRequest(
        String id,
        String idProducto,
        BigDecimal cantidad
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public static ActualizarIngredienteRecetaRequest createFromRequest(Map<String, Object> request){
        String id = Validador.textoOpcional(request.get("id"), "id");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;

        return new ActualizarIngredienteRecetaRequest(id, idProducto, cantidad);
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
}
