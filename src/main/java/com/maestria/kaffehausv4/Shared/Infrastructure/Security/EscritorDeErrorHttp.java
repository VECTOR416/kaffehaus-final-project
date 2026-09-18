package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Domain.Error.BaseException;
import com.maestria.kaffehausv4.Shared.Presentation.GlobalExceptionHandler.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Escribe un error de seguridad directamente en la respuesta, con la misma forma que el resto
 * de errores del sistema.
 *
 * Hace falta porque la cadena de filtros corre **antes** que el DispatcherServlet: cuando el
 * filtro de JWT o el motor de autorizacion rechazan una peticion, la excepcion no pasa nunca
 * por @RestControllerAdvice y GlobalExceptionHandler no puede traducirla. Sin esta clase, un
 * 401 devolveria el HTML de error del contenedor y el cliente tendria que tratar dos formatos
 * de error distintos.
 *
 * Reutiliza el record ErrorResponse del propio GlobalExceptionHandler a proposito: si algun
 * dia cambia el contrato de error, cambia en los dos sitios a la vez o no compila.
 */
@Component
public class EscritorDeErrorHttp {

    private final ObjectMapper objectMapper;

    public EscritorDeErrorHttp(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Traduce una excepcion del sistema a su respuesta JSON. La via habitual. */
    public void escribir(HttpServletResponse response, BaseException ex) throws IOException {
        escribir(response, ex.getCodigo(), ex.getTipo(), ex.getMensaje());
    }

    public void escribir(HttpServletResponse response, int codigo, String tipo, String mensaje) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(codigo);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorResponse(codigo, tipo, mensaje, OffsetDateTime.now()));
    }
}
