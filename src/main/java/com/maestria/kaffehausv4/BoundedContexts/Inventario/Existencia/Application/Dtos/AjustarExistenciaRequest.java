package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Ajuste por inventario fisico (inventario.md INV-43). Se envia lo contado, no la diferencia:
 * la diferencia es derivada y la calcula la existencia.
 */
public class AjustarExistenciaRequest {
    private BigDecimal contenidoContado;
    private String motivo;
    private String idUsuario;

    private AjustarExistenciaRequest(BigDecimal contenidoContado, String motivo, String idUsuario) {
        this.contenidoContado = contenidoContado;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static AjustarExistenciaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        BigDecimal contenidoContado = BigDecimal.valueOf(Validador.numeroRequerido(request.get("contenidoContado"), "contenidoContado"));
        String motivo = Validador.textoRequerido(request.get("motivo"), "motivo");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        return new AjustarExistenciaRequest(contenidoContado, motivo, idUsuario);
    }

    public BigDecimal getContenidoContado() {return contenidoContado;}
    public String getMotivo() {return motivo;}
    public String getIdUsuario() {return idUsuario;}
}
