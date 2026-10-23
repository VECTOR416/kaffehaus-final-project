package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Baja por vencimiento de las unidades de una linea de lote (logica.md VENCIMIENTO-03). */
public class DarDeBajaVencidoDetalleLoteRequest {
    /** Motivo del movimiento cuando el usuario no envia uno. */
    public static final String MOTIVO_POR_DEFECTO = "Baja por vencimiento";

    private String idUsuario;
    private String motivo;

    private DarDeBajaVencidoDetalleLoteRequest(String idUsuario, String motivo) {
        this.idUsuario = idUsuario;
        this.motivo = motivo;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     * 'motivo' es opcional.
     */
    public static DarDeBajaVencidoDetalleLoteRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        return new DarDeBajaVencidoDetalleLoteRequest(idUsuario, motivo != null ? motivo : MOTIVO_POR_DEFECTO);
    }

    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
}
