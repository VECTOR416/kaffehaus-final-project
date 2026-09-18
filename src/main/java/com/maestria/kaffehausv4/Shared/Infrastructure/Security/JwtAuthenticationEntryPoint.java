package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Respuesta a una peticion sin credenciales contra una ruta que si las exige: 401.
 *
 * Sustituye al comportamiento por defecto de Spring Security, que para una API REST es el
 * equivocado: devolver 'WWW-Authenticate' o redirigir a un formulario de login que aqui no
 * existe. Devuelve el mismo ErrorResponse que el resto del sistema.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String SIN_CREDENCIALES =
            "Falta la cabecera Authorization con un token Bearer valido";

    private final EscritorDeErrorHttp escritorDeErrorHttp;

    public JwtAuthenticationEntryPoint(EscritorDeErrorHttp escritorDeErrorHttp) {
        this.escritorDeErrorHttp = escritorDeErrorHttp;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        escritorDeErrorHttp.escribir(response, new AutorizacionException(401, SIN_CREDENCIALES));
    }
}
