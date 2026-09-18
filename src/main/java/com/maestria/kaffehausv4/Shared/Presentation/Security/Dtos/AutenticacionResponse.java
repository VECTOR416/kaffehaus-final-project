package com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos;

import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioAutenticado;

/**
 * Respuesta unica de los dos flujos de login: un poco de informacion del usuario y los dos
 * tokens.
 *
 * Que el login clasico y el de Google devuelvan exactamente esta forma es parte del contrato:
 * el cliente no deberia distinguir por donde entro el usuario.
 *
 * El bloque 'usuario' es el mismo objeto que se firma dentro del access token, de modo que lo
 * que el cliente muestra en pantalla y lo que el token afirma no pueden desincronizarse.
 */
public record AutenticacionResponse(
        UsuarioAutenticado usuario,
        String accessToken,
        String refreshToken) {
}
