package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Anulacion de una venta (ventas.md VEN-40). 'reintegrar' es falso por defecto: lo normal es
 * que el producto ya se entrego o se preparo (VEN-41).
 */
public class AnularVentaRequest {
    private String idUsuario;
    private String motivo;
    private boolean reintegrar;

    private AnularVentaRequest(String idUsuario, String motivo, boolean reintegrar) {
        this.idUsuario = idUsuario;
        this.motivo = motivo;
        this.reintegrar = reintegrar;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static AnularVentaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String motivo = Validador.textoRequerido(request.get("motivo"), "motivo");
        Boolean reintegrar = Validador.booleanoOpcional(request.get("reintegrar"), "reintegrar");

        return new AnularVentaRequest(idUsuario, motivo, reintegrar != null && reintegrar);
    }

    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
    public boolean isReintegrar() {return reintegrar;}
}
