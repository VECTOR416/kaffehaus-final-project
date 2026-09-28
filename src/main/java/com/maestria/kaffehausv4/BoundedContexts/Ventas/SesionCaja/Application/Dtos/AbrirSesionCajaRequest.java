package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** El titular del turno es quien firma la peticion; no viaja en el cuerpo. */
public class AbrirSesionCajaRequest {
    private BigDecimal montoApertura;
    private String idUsuario;

    private AbrirSesionCajaRequest(BigDecimal montoApertura, String idUsuario) {
        this.montoApertura = montoApertura;
        this.idUsuario = idUsuario;
    }

    public static AbrirSesionCajaRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion) {
        BigDecimal montoApertura = BigDecimal.valueOf(Validador.numeroRequerido(request.get("montoApertura"), "montoApertura"));
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");

        return new AbrirSesionCajaRequest(montoApertura, idUsuario);
    }

    public BigDecimal getMontoApertura() {return montoApertura;}
    public String getIdUsuario() {return idUsuario;}
}
