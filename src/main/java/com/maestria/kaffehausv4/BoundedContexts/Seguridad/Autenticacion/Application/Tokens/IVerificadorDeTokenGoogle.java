package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens;

/**
 * Puerto de verificacion del id_token de Google.
 *
 * Devuelve unicamente el correo porque es lo unico que el flujo necesita: el usuario debe
 * existir ya en la base, no se provisiona nada con el resto del perfil.
 */
public interface IVerificadorDeTokenGoogle {

    /**
     * Comprueba criptograficamente el token contra las claves publicas de Google y devuelve el
     * correo verificado que contiene.
     *
     * Lanza AutorizacionException 401 si la firma no valida, si expiro, si el destinatario no
     * es este cliente o si Google no da el correo por verificado.
     */
    String obtenerCorreoVerificado(String googleToken);
}
