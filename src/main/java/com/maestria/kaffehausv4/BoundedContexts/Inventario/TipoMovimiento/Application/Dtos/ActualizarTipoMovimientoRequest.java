package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarTipoMovimientoRequest {
    private String nombre;

    private ActualizarTipoMovimientoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarTipoMovimientoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarTipoMovimientoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
