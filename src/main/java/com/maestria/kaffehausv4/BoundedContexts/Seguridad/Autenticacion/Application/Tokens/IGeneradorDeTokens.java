package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;

/**
 * Puerto de emision y verificacion de los tokens propios del sistema.
 *
 * Se declara en Application/, como IHasheadorDeContrasena: el caso de uso lo recibe por
 * constructor y no sabe si debajo hay JWT, PASETO o tokens opacos.
 *
 * Los dos tipos de token se firman con secretos distintos a proposito: un refresh token
 * presentado como access token (o al reves) no debe validar jamas.
 */
public interface IGeneradorDeTokens {

    /** Access token de vida corta con el payload minimo: sub, nombreCompleto, rolId, rolNombre. */
    String emitirAccessToken(UsuarioAutenticado usuario);

    /** Refresh token de vida larga; solo necesita identificar al usuario. */
    String emitirRefreshToken(String usuarioId);

    /**
     * Verifica firma y vigencia de un access token y devuelve a quien identifica.
     * Lanza AutorizacionException 401 si es invalido, esta expirado o fue alterado.
     */
    UsuarioAutenticado verificarAccessToken(String accessToken);

    /**
     * Verifica firma y vigencia de un refresh token y devuelve el id de usuario que contiene.
     * Lanza AutorizacionException 401 en los mismos casos.
     */
    String verificarRefreshToken(String refreshToken);
}
