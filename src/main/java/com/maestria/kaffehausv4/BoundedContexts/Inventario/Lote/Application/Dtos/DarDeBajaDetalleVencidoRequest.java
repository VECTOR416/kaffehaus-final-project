package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Baja de las unidades cerradas de un detalle vencido (inventario.md INV-42). */
public class DarDeBajaDetalleVencidoRequest {
    private String idUsuario;
    private String motivo;

    private DarDeBajaDetalleVencidoRequest(String idUsuario, String motivo) {
        this.idUsuario = idUsuario;
        this.motivo = motivo;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static DarDeBajaDetalleVencidoRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        return new DarDeBajaDetalleVencidoRequest(idUsuario, motivo);
    }

    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
}
