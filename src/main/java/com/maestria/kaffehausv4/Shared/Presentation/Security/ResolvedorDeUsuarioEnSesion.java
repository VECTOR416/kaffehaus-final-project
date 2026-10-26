package com.maestria.kaffehausv4.Shared.Presentation.Security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Resuelve los parametros anotados con @UsuarioEnSesion: el id del usuario que dejo en el
 * SecurityContext JwtAuthenticationFilter al validar el token.
 *
 * Si la peticion llega sin autenticar responde 401. Con la cadena cerrada eso no deberia
 * ocurrir, pero un endpoint que necesita saber quien opera no puede seguir sin saberlo.
 */
@Component
public class ResolvedorDeUsuarioEnSesion implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(UsuarioEnSesion.class)
                && String.class.equals(parametro.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parametro,
            ModelAndViewContainer contenedor,
            NativeWebRequest peticion,
            WebDataBinderFactory fabricaDeBinders) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUsuarioId();
        }

        throw AutorizacionException.throwException(401, "La peticion no esta autenticada");
    }
}
