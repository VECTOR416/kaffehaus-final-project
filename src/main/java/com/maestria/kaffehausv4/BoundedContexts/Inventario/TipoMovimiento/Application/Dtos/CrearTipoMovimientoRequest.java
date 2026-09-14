package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearTipoMovimientoRequest {
    private String nombre;

    private CrearTipoMovimientoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearTipoMovimientoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearTipoMovimientoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
