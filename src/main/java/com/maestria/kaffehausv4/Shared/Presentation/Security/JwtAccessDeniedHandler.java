package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 403 cuando el token es valido pero el rol no alcanza para la operacion.
 *
 * Emite el mismo ErrorResponse que el resto de la API.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JwtAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        EscritorDeErrorDeSeguridad.escribir(
            response,
            jsonMapper,
            403,
            "No cuenta con los permisos necesarios para esta operacion"
        );
    }
}
