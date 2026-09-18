package com.maestria.kaffehausv4.Shared.Presentation.Security;

/**
 * Quien hace la peticion: exactamente lo que lleva el access token, ni un campo mas.
 *
 * Un JWT va firmado pero no cifrado; cualquiera que lo intercepte lee su contenido. Por eso
 * solo viajan cuatro datos, los mismos que devuelve el login: el id del usuario, su nombre
 * completo para mostrarlo, y el id y el nombre de su rol. Nada de correo, contrasena ni
 * permisos.
 *
 * El rol viaja por nombre porque asi es como lo guarda la tabla 'rol' y como lo nombran los
 * endpoints protegidos: @RequiereRol("Administrador"). No hay ningun catalogo de roles escrito
 * en el codigo; el rol sale de la base en cada login y en cada refresh.
 *
 * Es el mismo objeto que se serializa dentro de la respuesta de autenticacion, para que lo que
 * el cliente recibe y lo que el token afirma no puedan desincronizarse.
 */
public record UsuarioAutenticado(
        String usuarioId,
        String nombreCompleto,
        String rolId,
        String rolNombre) {

    /** true si su rol es alguno de los nombres indicados. Lo usa GuardiaDeRoles. */
    public boolean tieneAlgunRol(String... rolesPermitidos) {
        for (String rol : rolesPermitidos) {
            if (rol != null && rol.equalsIgnoreCase(rolNombre)) {
                return true;
            }
        }
        return false;
    }
}
