package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Apertura de una unidad cerrada elegida por el usuario (inventario.md INV-14). */
public class AbrirDetalleLoteRequest {
    private String idUsuario;

    private AbrirDetalleLoteRequest(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static AbrirDetalleLoteRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        return new AbrirDetalleLoteRequest(idUsuario);
    }

    public String getIdUsuario() {return idUsuario;}
}
