package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Paso del payload de alta de una receta. Viaja anidado dentro de CrearRecetaRequest.
 *
 * No admite el numero de paso: la posicion la asigna la raiz segun el orden en que llegan los
 * elementos del array, que es la unica forma de garantizar la secuencia 1..N.
 */
public class PasoRecetaRequest {
    private String descripcion;

    private PasoRecetaRequest(String descripcion) {
        this.descripcion = descripcion;
    }

    public static PasoRecetaRequest createFromRequest(Map<String, Object> request){
        String descripcion = Validador.textoRequerido(request.get("descripcion"), "descripcion");

        return new PasoRecetaRequest(descripcion);
    }

    public String getDescripcion() {return descripcion;}
}
