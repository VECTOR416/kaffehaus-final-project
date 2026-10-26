package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Baja por vencimiento del contenido de una existencia (logica.md VENCIMIENTO-03). */
public class DarDeBajaVencidaExistenciaRequest {
    /** Motivo del movimiento cuando el usuario no envia uno. */
    public static final String MOTIVO_POR_DEFECTO = "Baja por vencimiento";

    private String idUsuario;
    private String motivo;

    private DarDeBajaVencidaExistenciaRequest(String idUsuario, String motivo) {
        this.idUsuario = idUsuario;
        this.motivo = motivo;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     * 'motivo' es opcional.
     */
    public static DarDeBajaVencidaExistenciaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        return new DarDeBajaVencidaExistenciaRequest(idUsuario, motivo != null ? motivo : MOTIVO_POR_DEFECTO);
    }

    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
}
