package com.maestria.kaffehausv4.Shared.Application.Security;

import com.maestria.kaffehausv4.Shared.Domain.Security.KeycloakId;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Puerto de ENTRADA de identidad: responde "quien esta ejecutando esta peticion".
 *
 * Ubicado en Application/ y no en Domain/, replicando la decision ya tomada en el
 * proyecto para IEntidadRepository e IEntidadDatasource.
 *
 * Un caso de uso lo recibe por constructor igual que recibe IEntidadRepository, y
 * nunca llega a saber que por debajo hay un JWT ni un SecurityContextHolder.
 *
 * Este puerto devuelve la identidad del TOKEN. La identidad de NEGOCIO (agregado
 * Usuario + Persona) se resuelve en el Bounded Context Usuario a partir del
 * KeycloakId, porque Shared no puede depender de un Bounded Context.
 */
public interface IContextoDeSeguridad {

    /** true si hay un usuario autenticado en el contexto de la peticion actual. */
    boolean hayUsuarioAutenticado();

    /**
     * Identificador del usuario en el proveedor de identidad (claim 'sub'), que se
     * corresponde con la columna 'usuario.keycloak_id'.
     *
     * @throws com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException 401 si no hay usuario autenticado
     */
    KeycloakId obtenerIdDeUsuarioActual();

    /**
     * Identidad completa del token: id, username, correo y roles.
     *
     * @throws com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException 401 si no hay usuario autenticado
     */
    UsuarioAutenticado obtenerUsuarioActual();

    /** Comprobacion puntual de rol dentro de un caso de uso. No lanza si no hay usuario: devuelve false. */
    boolean tieneRol(RolDeSistema rol);
}
