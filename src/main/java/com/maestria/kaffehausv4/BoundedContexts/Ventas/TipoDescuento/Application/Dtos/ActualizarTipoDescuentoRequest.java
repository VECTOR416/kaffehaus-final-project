package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarTipoDescuentoRequest {
    private String nombre;

    private ActualizarTipoDescuentoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarTipoDescuentoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarTipoDescuentoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
