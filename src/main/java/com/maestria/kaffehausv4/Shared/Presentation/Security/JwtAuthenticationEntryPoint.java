package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 401 cuando el token esta ausente, expirado o es invalido.
 *
 * Emite el mismo ErrorResponse que el resto de la API. Los mensajes van en espanol
 * y sin tildes, como en Validador y en los DomainException del proyecto.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public JwtAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        EscritorDeErrorDeSeguridad.escribir(
            response,
            jsonMapper,
            401,
            "Token de acceso ausente o invalido"
        );
    }
}
