package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearEstadoPedidoRequest {
    private String nombre;

    private CrearEstadoPedidoRequest(
        String nombre
    ) {
        this.nombre = nombre;
    }

    public static CrearEstadoPedidoRequest createFromRequest(Map<String, Object> request){
        String nombre = Validador.textoRequerido(request.get("nombre"), "nombre");

        return new CrearEstadoPedidoRequest(nombre);
    }

    public String getNombre() {return nombre;}
}
