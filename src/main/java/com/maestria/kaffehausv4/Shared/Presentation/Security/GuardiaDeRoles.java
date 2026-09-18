package com.maestria.kaffehausv4.Shared.Presentation.Security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica el decorador @RequiereRol.
 *
 * Corre despues de la cadena de filtros, ya dentro de Spring MVC, asi que cuando llega aqui el
 * usuario ya esta autenticado: JwtAuthenticationFilter valido el token y lo dejo en el
 * SecurityContext. Lo unico que queda por decidir es si su rol alcanza.
 *
 * Es un interceptor y no una regla de la cadena de seguridad a proposito: aqui se conoce el
 * metodo del controller que va a atender la peticion, y por tanto sus anotaciones. Una regla
 * por URL solo conoce la ruta, y obligaria a mantener una tabla de rutas en paralelo a los
 * controllers.
 *
 * No escribe la respuesta de error a mano: lanza AutorizacionException y deja que
 * GlobalExceptionHandler la traduzca, para que un 403 de aqui tenga la misma forma que
 * cualquier otro error del sistema.
 */
@Component
public class GuardiaDeRoles implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiereRol anotacion = buscarAnotacion(handlerMethod);

        if (anotacion == null) {
            return true;
        }

        UsuarioAutenticado usuario = usuarioDeLaPeticion();

        if (usuario == null) {
            throw AutorizacionException.throwException(401, "La peticion no esta autenticada");
        }

        if (!usuario.tieneAlgunRol(anotacion.value())) {
            throw AutorizacionException.throwException(403,
                    "El rol '" + usuario.rolNombre() + "' no tiene acceso a este recurso");
        }

        return true;
    }

    /** La anotacion del metodo manda; si no la hay, se mira la de la clase del controller. */
    private RequiereRol buscarAnotacion(HandlerMethod handlerMethod) {
        RequiereRol anotacion = handlerMethod.getMethodAnnotation(RequiereRol.class);
        return anotacion != null ? anotacion : handlerMethod.getBeanType().getAnnotation(RequiereRol.class);
    }

    /** Quien firma la peticion, o null si llego sin autenticar. */
    private UsuarioAutenticado usuarioDeLaPeticion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUsuario();
        }

        return null;
    }
}
