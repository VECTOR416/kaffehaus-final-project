package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Adaptador entre el usuario del sistema y Spring Security.
 *
 * Es la unica clase que traduce nuestra identidad (UsuarioAutenticado: el id de la fila
 * 'usuario', su nombre y su rol, tal como se leyeron de la base al iniciar sesion) al contrato
 * UserDetails que exige el framework. Gracias a este envoltorio ninguna entidad de dominio
 * implementa UserDetails ni conoce GrantedAuthority: el dominio queda puro y todo el
 * acoplamiento con Spring Security se concentra en esta capa externa.
 *
 * Autoridades: se concede una sola, 'ROLE_' + el nombre del rol normalizado, y ese nombre sale
 * del claim 'rolNombre', que a su vez es el valor de 'rol.nombre' en la base. Los roles no
 * estan escritos a mano en la capa de seguridad: se leen de la tabla 'rol' durante el login y
 * viajan firmados en el token, asi que anadir una fila a 'rol' basta para que un .hasRole(...)
 * pueda referirse a ella.
 *
 * getPassword() devuelve null a proposito: la API es stateless y autentica por Bearer token,
 * de modo que el hash de la contrasena no llega nunca hasta aqui. Esa comprobacion vive en el
 * caso de uso de login, con IHasheadorDeContrasena.
 *
 * Se inyecta en los controllers con @AuthenticationPrincipal SecurityUser, que es el
 * equivalente idiomatico del req.user de otros entornos.
 */
public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final transient UsuarioAutenticado usuario;

    public SecurityUser(UsuarioAutenticado usuario) {
        this.usuario = usuario;
    }

    /** El usuario del sistema que hay debajo del envoltorio, para el codigo de negocio. */
    public UsuarioAutenticado getUsuario() {
        return usuario;
    }

    public String getUsuarioId() {
        return usuario.getUsuarioId();
    }

    public String getNombreCompleto() {
        return usuario.getNombreCompleto();
    }

    public int getRolId() {
        return usuario.getRolId();
    }

    public String getRolNombre() {
        return usuario.getRolNombre();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(
                RolDelSistema.PREFIJO_DE_AUTORIDAD + RolDelSistema.aNombreDeAutoridad(usuario.getRolNombre())));
    }

    /** No aplica: la autenticacion es por token y la contrasena no interviene. */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * El identificador tecnico, no el correo: es lo que viaja en el 'sub' del token y lo que
     * usa el resto del sistema para referirse a un usuario.
     */
    @Override
    public String getUsername() {
        return usuario.getUsuarioId();
    }
}
