package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

/**
 * Nombres de los atributos que el guard deja en la peticion HTTP.
 *
 * Son el equivalente al req.user de Express: cualquier filtro, interceptor o codigo que tenga
 * a mano el HttpServletRequest puede leer quien la firmo sin depender del ThreadLocal.
 *
 * Se declaran como constantes y no como literales sueltos para que el nombre del atributo sea
 * uno solo y el compilador avise si se renombra.
 */
public final class AtributosDeSeguridad {

    /** El UsuarioAutenticado completo. request.getAttribute("user"). */
    public static final String USUARIO = "user";

    /** El id del usuario, como String. request.getAttribute("usuarioId"). */
    public static final String USUARIO_ID = "usuarioId";

    /** El codigo del rol (1 Operador, 2 Administrador), como Integer. request.getAttribute("rolId"). */
    public static final String ROL_ID = "rolId";

    /** El nombre del rol. request.getAttribute("rolNombre"). */
    public static final String ROL_NOMBRE = "rolNombre";

    private AtributosDeSeguridad() {}
}
