package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/** Credencial de Google: el id_token que el cliente obtuvo del proveedor. */
public class IniciarSesionConGoogleRequest {
    private String googleToken;

    private IniciarSesionConGoogleRequest(String googleToken) {
        this.googleToken = googleToken;
    }

    public static IniciarSesionConGoogleRequest createFromRequest(Map<String, Object> request){
        String googleToken = Validador.textoRequerido(request.get("googleToken"), "googleToken");

        return new IniciarSesionConGoogleRequest(googleToken);
    }

    public String getGoogleToken() {return googleToken;}
}
