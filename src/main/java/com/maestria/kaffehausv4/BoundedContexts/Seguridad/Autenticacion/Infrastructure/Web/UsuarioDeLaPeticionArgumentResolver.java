package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.UsuarioDeLaPeticion;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve los parametros anotados con @UsuarioDeLaPeticion.
 *
 * Lee el atributo que dejo el guard en la peticion, no el ThreadLocal: asi la inyeccion
 * funciona igual si algun dia el proyecto pasa a un modelo donde el hilo no acompana a la
 * peticion de principio a fin.
 *
 * Soporta dos formas del parametro:
 * - UsuarioAutenticado: obligatorio, 401 si la peticion no venia autenticada.
 * - Optional&lt;UsuarioAutenticado&gt;: opcional, vacio si no lo venia.
 */
@Component
public class UsuarioDeLaPeticionArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(UsuarioDeLaPeticion.class)) {
            return false;
        }

        Class<?> tipo = parameter.getParameterType();
        return UsuarioAutenticado.class.equals(tipo) || Optional.class.equals(tipo);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object atributo = request != null ? request.getAttribute(AtributosDeSeguridad.USUARIO) : null;
        UsuarioAutenticado usuario = atributo instanceof UsuarioAutenticado autenticado ? autenticado : null;

        if (Optional.class.equals(parameter.getParameterType())) {
            return Optional.ofNullable(usuario);
        }

        if (usuario == null) {
            throw AutorizacionException.throwException(401, "La peticion no esta autenticada");
        }

        return usuario;
    }
}
