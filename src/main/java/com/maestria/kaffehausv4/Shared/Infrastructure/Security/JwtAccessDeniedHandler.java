package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Respuesta a una peticion autenticada cuyo rol no alcanza para el recurso: 403.
 *
 * La distincion con el 401 es la de siempre y conviene no perderla: 401 es "no se quien eres",
 * 403 es "se quien eres y no te toca". El mensaje nombra el rol para que el cliente pueda
 * explicarselo al usuario, pero no dice que rol haria falta: eso es informacion sobre la
 * estructura de permisos del sistema.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final EscritorDeErrorHttp escritorDeErrorHttp;

    public JwtAccessDeniedHandler(EscritorDeErrorHttp escritorDeErrorHttp) {
        this.escritorDeErrorHttp = escritorDeErrorHttp;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        escritorDeErrorHttp.escribir(response, new AutorizacionException(403, mensaje()));
    }

    private String mensaje() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.getPrincipal() instanceof SecurityUser usuario) {
            return "El rol '" + usuario.getRolNombre() + "' no tiene acceso a este recurso";
        }

        return "No tiene acceso a este recurso";
    }
}
