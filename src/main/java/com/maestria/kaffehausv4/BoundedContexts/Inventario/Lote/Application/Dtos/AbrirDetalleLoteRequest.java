package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Apertura de una unidad cerrada elegida por el usuario (inventario.md INV-14). */
public class AbrirDetalleLoteRequest {
    private String idUsuario;

    private AbrirDetalleLoteRequest(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public static AbrirDetalleLoteRequest createFromRequest(Map<String, Object> request) {
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");
        return new AbrirDetalleLoteRequest(idUsuario);
    }

    public String getIdUsuario() {return idUsuario;}
}
