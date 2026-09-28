package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Baja por vencimiento de todo el contenido de una existencia (inventario.md INV-42). */
public class RegistrarVencimientoRequest {
    private String motivo;
    private String idUsuario;

    private RegistrarVencimientoRequest(String motivo, String idUsuario) {
        this.motivo = motivo;
        this.idUsuario = idUsuario;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static RegistrarVencimientoRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        return new RegistrarVencimientoRequest(motivo, idUsuario);
    }

    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
}
