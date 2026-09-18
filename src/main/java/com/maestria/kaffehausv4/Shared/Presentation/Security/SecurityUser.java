package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador entre el usuario del sistema y Spring Security.
 *
 * Es la unica clase que traduce nuestra identidad (UsuarioAutenticado, con los datos que se
 * leyeron de las tablas 'usuario', 'persona' y 'rol' al iniciar sesion) al contrato UserDetails
 * que exige el framework. Gracias a este envoltorio ninguna entidad de dominio implementa
 * UserDetails ni conoce GrantedAuthority.
 *
 * La autoridad se deriva del nombre del rol que trae el token, que es el valor de 'rol.nombre'
 * en la base: 'Administrador' concede ROLE_ADMINISTRADOR. Asi se puede usar .hasRole(...) en la
 * cadena de filtros si algun dia hace falta, aunque la proteccion de endpoints del proyecto se
 * escribe con @RequiereRol, que compara contra el nombre tal cual.
 *
 * getPassword() devuelve null a proposito: la API es stateless y autentica por Bearer token, de
 * modo que el hash de la contrasena no llega nunca hasta aqui. Esa comprobacion vive en
 * AutenticacionService, con IHasheadorDeContrasena.
 *
 * Se inyecta en los controllers con @AuthenticationPrincipal SecurityUser, que es el
 * equivalente idiomatico del req.user de otros entornos.
 */
public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** Prefijo que Spring Security exige para que .hasRole(...) encuentre la autoridad. */
    private static final String PREFIJO_DE_ROL = "ROLE_";

    private final transient UsuarioAutenticado usuario;

    public SecurityUser(UsuarioAutenticado usuario) {
        this.usuario = usuario;
    }

    /** El usuario del sistema que hay debajo del envoltorio. */
    public UsuarioAutenticado getUsuario() {
        return usuario;
    }

    public String getUsuarioId() {
        return usuario.usuarioId();
    }

    public String getNombreCompleto() {
        return usuario.nombreCompleto();
    }

    public String getRolId() {
        return usuario.rolId();
    }

    public String getRolNombre() {
        return usuario.rolNombre();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String nombre = usuario.rolNombre() == null ? "" : usuario.rolNombre();
        return List.of(new SimpleGrantedAuthority(
                PREFIJO_DE_ROL + nombre.trim().toUpperCase(Locale.ROOT).replace(' ', '_')));
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
        return usuario.usuarioId();
    }
}
