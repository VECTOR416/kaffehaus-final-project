package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects;

/**
 * Identidad de quien hace la peticion: exactamente lo que lleva el access token, ni un campo
 * mas.
 *
 * Un JWT va firmado pero no cifrado; cualquiera que lo intercepte lee su contenido. Por eso el
 * payload se limita a lo imprescindible para autorizar sin volver a la base: el id del
 * usuario, su nombre para mostrarlo, y su rol. Nada de correo, telefono ni permisos.
 *
 * El guard lo construye al validar el token y lo deja disponible para la peticion en curso.
 */
public class UsuarioAutenticado {

    private final String usuarioId;
    private final String nombreCompleto;
    private final int rolId;
    private final String rolNombre;

    public UsuarioAutenticado(String usuarioId, String nombreCompleto, int rolId, String rolNombre) {
        this.usuarioId = usuarioId;
        this.nombreCompleto = nombreCompleto;
        this.rolId = rolId;
        this.rolNombre = rolNombre;
    }

    /** true si quien hace la peticion tiene alguno de los roles exigidos por el endpoint. */
    public boolean tieneAlgunRol(RolDelSistema... rolesPermitidos) {
        for (RolDelSistema rol : rolesPermitidos) {
            if (rol.tieneCodigo(this.rolId)) {
                return true;
            }
        }
        return false;
    }

    public String getUsuarioId() {return usuarioId;}
    public String getNombreCompleto() {return nombreCompleto;}
    public int getRolId() {return rolId;}
    public String getRolNombre() {return rolNombre;}
}
