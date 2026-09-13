package com.maestria.kaffehausv4.Shared.Application.Security;

import com.maestria.kaffehausv4.Shared.Domain.Security.KeycloakId;

/**
 * Puerto de SALIDA hacia el proveedor de identidad: propaga altas, bajas y cambios
 * de usuarios y roles desde el backend hacia la Admin REST API.
 *
 * Sufijo 'Datasource' por coherencia con IEntidadDatasource / IUsuarioDatasource: es
 * una fuente de datos externa, solo que el adaptador termina en HTTP y no en JPA.
 *
 * DETALLE CRITICO DEL DISENO: este puerto habla en tipos primitivos y Value Objects
 * de Shared, NUNCA en agregados (Usuario, Rol). Eso es lo que le permite vivir en el
 * kernel compartido sin que Shared dependa de ningun Bounded Context.
 *
 * Ningun tipo propio de Keycloak (UserRepresentation, RoleRepresentation) ni ninguna
 * excepcion de HTTP cruza esta frontera: el adaptador las traduce a DomainException /
 * SystemException para que el contrato de error de la API siga siendo uno solo.
 */
public interface IIdentidadExternaDatasource {

    // ----- Usuarios -----

    /**
     * Crea el usuario y devuelve el identificador generado por el proveedor.
     *
     * Ese identificador es el que el caso de uso debe guardar en 'usuario.keycloak_id',
     * y es la razon por la que la sincronizacion se orquesta en el caso de uso y no
     * se esconde detras del repositorio: las dos escrituras estan ordenadas y son
     * dependientes en datos.
     *
     * @param contrasenaTemporal credencial inicial; se marca como temporal para forzar
     *                           su cambio en el primer inicio de sesion. Nunca se persiste local.
     */
    KeycloakId crearUsuario(String correo, String nombres, String apellidos, String contrasenaTemporal);

    void actualizarUsuario(KeycloakId usuarioId, String correo, String nombres, String apellidos);

    void cambiarContrasena(KeycloakId usuarioId, String nuevaContrasena);

    /** Contraparte de Usuario.reactivar(): vuelve a habilitar el acceso. */
    void habilitarUsuario(KeycloakId usuarioId);

    /**
     * Contraparte de Usuario.eliminar(): el borrado logico local ('borrado = true') se
     * corresponde con 'enabled = false' en el proveedor, no con un borrado real.
     */
    void deshabilitarUsuario(KeycloakId usuarioId);

    /**
     * Borrado fisico. Reservado para la COMPENSACION: si la escritura en la base de
     * datos local falla despues de haber creado el usuario en el proveedor, el caso de
     * uso invoca este metodo para no dejar un huerfano.
     */
    void eliminarUsuario(KeycloakId usuarioId);

    void asignarRol(KeycloakId usuarioId, String nombreRol);

    void quitarRol(KeycloakId usuarioId, String nombreRol);

    // ----- Roles -----

    void crearRol(String nombre);

    void renombrarRol(String nombreActual, String nombreNuevo);

    void eliminarRol(String nombre);

    boolean existeRol(String nombre);
}
