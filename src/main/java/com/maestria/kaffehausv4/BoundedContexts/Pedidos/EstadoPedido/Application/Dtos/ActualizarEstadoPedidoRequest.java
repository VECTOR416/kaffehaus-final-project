package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarEstadoPedidoRequest {
    private String nombre;

    private ActualizarEstadoPedidoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static ActualizarEstadoPedidoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoOpcional(request.get("nombre"), "nombre");

        return new ActualizarEstadoPedidoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
