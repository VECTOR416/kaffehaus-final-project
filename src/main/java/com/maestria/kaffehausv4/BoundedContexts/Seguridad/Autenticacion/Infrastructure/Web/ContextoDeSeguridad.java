package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Quien hace la peticion en curso: el equivalente al req.user de otros entornos.
 *
 * Se apoya en un ThreadLocal porque el proyecto es Spring MVC sobre servlets, donde cada
 * peticion se atiende en su propio hilo. El interceptor lo llena en preHandle y, sobre todo,
 * lo limpia en afterCompletion: los hilos se reutilizan entre peticiones y dejar ahi al
 * usuario anterior significaria atribuirle a alguien acciones que no hizo.
 */
public final class ContextoDeSeguridad {

    private static final ThreadLocal<UsuarioAutenticado> USUARIO = new ThreadLocal<>();

    private ContextoDeSeguridad() {}

    static void establecer(UsuarioAutenticado usuario) {
        USUARIO.set(usuario);
    }

    static void limpiar() {
        USUARIO.remove();
    }

    /** Devuelve el usuario de la peticion, o null si el endpoint no exige autenticacion. */
    public static UsuarioAutenticado usuarioActual() {
        return USUARIO.get();
    }

    /**
     * Devuelve el usuario de la peticion y falla con 401 si no hay ninguno. Para usar dentro
     * de endpoints anotados, donde la ausencia de usuario seria un error de programacion.
     */
    public static UsuarioAutenticado exigirUsuarioActual() {
        UsuarioAutenticado usuario = USUARIO.get();
        if (usuario == null) {
            throw AutorizacionException.throwException(401, "La peticion no esta autenticada");
        }
        return usuario;
    }
}
