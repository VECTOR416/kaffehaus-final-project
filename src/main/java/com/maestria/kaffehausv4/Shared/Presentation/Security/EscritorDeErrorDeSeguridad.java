package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;

import com.maestria.kaffehausv4.Shared.Presentation.GlobalExceptionHandler;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializa un error de seguridad con el MISMO formato que GlobalExceptionHandler.
 *
 * Hace falta porque los 401 y 403 se producen dentro de la cadena de filtros, antes
 * del DispatcherServlet: @RestControllerAdvice nunca los ve, y sin esto Spring
 * Security devolveria una respuesta vacia con cabecera WWW-Authenticate, rompiendo
 * la uniformidad del contrato de error de la API.
 *
 * Reutiliza el propio record GlobalExceptionHandler.ErrorResponse en lugar de
 * duplicarlo, de modo que el formato no puede divergir.
 *
 * Usa JsonMapper de Jackson 3 (tools.jackson), que es lo que Spring Boot 4 trae y
 * auto-configura. Jackson 2 (com.fasterxml) NO esta en el classpath de este proyecto
 * y su ObjectMapper no existe como bean.
 */
final class EscritorDeErrorDeSeguridad {

    private EscritorDeErrorDeSeguridad() {}

    static void escribir(
        HttpServletResponse response,
        JsonMapper jsonMapper,
        int codigo,
        String mensaje
    ) throws IOException {
        GlobalExceptionHandler.ErrorResponse body = new GlobalExceptionHandler.ErrorResponse(
            codigo,
            "Authorization Exception",
            mensaje,
            OffsetDateTime.now()
        );

        response.setStatus(codigo);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
