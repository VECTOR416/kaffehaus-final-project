package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens.IGeneradorDeTokens;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereAutenticacion;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereRol;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final IGeneradorDeTokens generadorDeTokens;

    public AutenticacionInterceptor(IGeneradorDeTokens generadorDeTokens) {
        this.generadorDeTokens = generadorDeTokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiereRol anotacionDeRol = buscarAnotacion(handlerMethod, RequiereRol.class);
        boolean exigeAutenticacion = buscarAnotacion(handlerMethod, RequiereAutenticacion.class) != null;

        if (anotacionDeRol == null && !exigeAutenticacion) {
            return true;
        }

        UsuarioAutenticado usuario = generadorDeTokens.verificarAccessToken(extraerToken(request));

        if (anotacionDeRol != null && !usuario.tieneAlgunRol(anotacionDeRol.value())) {
            throw AutorizacionException.throwException(403,
                    "El rol '" + usuario.getRolNombre() + "' no tiene acceso a este recurso");
        }

        ContextoDeSeguridad.establecer(usuario);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Imprescindible: el hilo se reutiliza para otras peticiones.
        ContextoDeSeguridad.limpiar();
    }

    /** La anotacion del metodo manda; si no la hay, se mira la de la clase del controller. */
    private <A extends java.lang.annotation.Annotation> A buscarAnotacion(HandlerMethod handlerMethod, Class<A> tipo) {
        A anotacion = handlerMethod.getMethodAnnotation(tipo);
        return anotacion != null ? anotacion : handlerMethod.getBeanType().getAnnotation(tipo);
    }

    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA);

        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            throw AutorizacionException.throwException(401,
                    "Falta la cabecera Authorization con un token Bearer");
        }

        return cabecera.substring(PREFIJO.length()).trim();
    }
}
