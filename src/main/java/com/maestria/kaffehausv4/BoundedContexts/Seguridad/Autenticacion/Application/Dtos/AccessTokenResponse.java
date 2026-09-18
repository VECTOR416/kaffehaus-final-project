package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

/** Respuesta de la renovacion: solo el access token nuevo, como fija la especificacion. */
public class AccessTokenResponse {
    private String accessToken;

    private AccessTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public static AccessTokenResponse create(String accessToken) {
        return new AccessTokenResponse(accessToken);
    }

    public String getAccessToken() {return accessToken;}
}
