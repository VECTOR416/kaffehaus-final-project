package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

/**
 * Respuesta unica de los dos flujos de login. Que el clasico y el de Google devuelvan
 * exactamente esta forma es parte del contrato: el cliente no deberia distinguir por donde
 * entro el usuario.
 */
public class AutenticacionResponse {
    private UsuarioAutenticadoResponse usuario;
    private String accessToken;
    private String refreshToken;

    private AutenticacionResponse(
        UsuarioAutenticadoResponse usuario,
        String accessToken,
        String refreshToken
    ) {
        this.usuario = usuario;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static AutenticacionResponse create(
            UsuarioAutenticadoResponse usuario,
            String accessToken,
            String refreshToken) {
        return new AutenticacionResponse(usuario, accessToken, refreshToken);
    }

    public UsuarioAutenticadoResponse getUsuario() {return usuario;}
    public String getAccessToken() {return accessToken;}
    public String getRefreshToken() {return refreshToken;}
}
