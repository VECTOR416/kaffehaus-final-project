package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Entrada de la renovacion: el refresh token emitido en el login. */
public class RenovarTokenRequest {
    private String refreshToken;

    private RenovarTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static RenovarTokenRequest createFromRequest(Map<String, Object> request){
        String refreshToken = Validador.textoRequerido(request.get("refreshToken"), "refreshToken");

        return new RenovarTokenRequest(refreshToken);
    }

    public String getRefreshToken() {return refreshToken;}
}
