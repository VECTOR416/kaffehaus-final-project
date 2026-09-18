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

/**
 * Guard de autenticacion y de roles.
 *
 * Es el equivalente al middleware de otros entornos: lee 'Authorization: Bearer <token>',
 * valida el access token y deja a quien firma la peticion disponible de dos formas
 * complementarias:
 *
 * - Como atributos de la peticion ('user', 'usuarioId', 'rolId', 'rolNombre'), al estilo del
 *   req.user de Express, para cualquier codigo que tenga el HttpServletRequest a mano.
 * - En ContextoDeSeguridad, para el codigo que no lo tiene.
 *
 * La **exigencia** de credenciales es opt-in: solo los handlers anotados con
 * @RequiereAutenticacion o @RequiereRol rechazan una peticion sin token. El resto de la API
 * sigue abierta, como estaba, para no cambiar en silencio el comportamiento de los controllers
 * existentes. Para cerrar todo por defecto basta con tratar 'exigeCredenciales' como true.
 *
 * La **lectura** de credenciales, en cambio, es incondicional: si llega una cabecera
 * Authorization se valida y se inyecta el usuario aunque el endpoint no lo exija. Eso es lo
 * que permite que un endpoint abierto se personalice cuando sabe quien llama
 * (@UsuarioDeLaPeticion Optional<UsuarioAutenticado>). Un token presente pero invalido es
 * siempre un 401, tambien en un endpoint abierto: ignorarlo en silencio esconderia al cliente
 * que su sesion caduco.
 *
 * No escribe la respuesta de error a mano: lanza AutorizacionException y deja que
 * GlobalExceptionHandler la traduzca, para que un 401 de aqui tenga la misma forma que
 * cualquier otro error del sistema.
 */
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
        boolean exigeCredenciales = anotacionDeRol != null
                || buscarAnotacion(handlerMethod, RequiereAutenticacion.class) != null;

        String token = extraerToken(request);

        if (token == null) {
            if (exigeCredenciales) {
                throw AutorizacionException.throwException(401,
                        "Falta la cabecera Authorization con un token Bearer");
            }
            return true;
        }

        UsuarioAutenticado usuario = generadorDeTokens.verificarAccessToken(token);

        if (anotacionDeRol != null && !usuario.tieneAlgunRol(anotacionDeRol.value())) {
            throw AutorizacionException.throwException(403,
                    "El rol '" + usuario.getRolNombre() + "' no tiene acceso a este recurso");
        }

        ContextoDeSeguridad.establecer(usuario);
        inyectarEnLaPeticion(request, usuario);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Imprescindible: el hilo se reutiliza para otras peticiones.
        ContextoDeSeguridad.limpiar();
    }

    /** Deja al usuario en la peticion, para quien prefiera leerlo de ahi y no del contexto. */
    private void inyectarEnLaPeticion(HttpServletRequest request, UsuarioAutenticado usuario) {
        request.setAttribute(AtributosDeSeguridad.USUARIO, usuario);
        request.setAttribute(AtributosDeSeguridad.USUARIO_ID, usuario.getUsuarioId());
        request.setAttribute(AtributosDeSeguridad.ROL_ID, usuario.getRolId());
        request.setAttribute(AtributosDeSeguridad.ROL_NOMBRE, usuario.getRolNombre());
    }

    /** La anotacion del metodo manda; si no la hay, se mira la de la clase del controller. */
    private <A extends java.lang.annotation.Annotation> A buscarAnotacion(HandlerMethod handlerMethod, Class<A> tipo) {
        A anotacion = handlerMethod.getMethodAnnotation(tipo);
        return anotacion != null ? anotacion : handlerMethod.getBeanType().getAnnotation(tipo);
    }

    /** Devuelve el token de la cabecera, o null si no viene ninguna cabecera Bearer. */
    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA);

        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return null;
        }

        return cabecera.substring(PREFIJO.length()).trim();
    }
}
