package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticacion por Bearer token. Es el **unico** sitio del proyecto donde se valida
 * un access token entrante.
 *
 * Su unica responsabilidad: si la peticion trae 'Authorization: Bearer <token>', verificarlo y
 * dejar al usuario en el SecurityContext. No decide quien puede entrar a donde; de los roles se
 * ocupa @RequiereRol a traves de GuardiaDeRoles.
 *
 * Extiende OncePerRequestFilter para garantizar una sola ejecucion por peticion aunque el
 * contenedor la reenvie internamente (forwards, error dispatches).
 *
 * Tres comportamientos deliberados:
 *
 * - Sin cabecera Authorization, la peticion sigue su curso sin autenticar. Quien la rechaza, si
 *   la ruta lo exige, es la cadena de seguridad, que es la que sabe si era publica o no.
 * - Con un token presente pero invalido o caducado, se responde 401 aqui mismo y se corta la
 *   cadena. Ignorarlo en silencio le escondaria al cliente que su sesion expiro.
 * - Las rutas de autenticacion se saltan el filtro entero: pedir un token nuevo no puede fallar
 *   por culpa del token viejo que el cliente aun tenga guardado.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final EscritorDeErrorHttp escritorDeErrorHttp;

    public JwtAuthenticationFilter(JwtService jwtService, EscritorDeErrorHttp escritorDeErrorHttp) {
        this.jwtService = jwtService;
        this.escritorDeErrorHttp = escritorDeErrorHttp;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(SecurityConfig.RUTA_DE_AUTENTICACION + "/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            autenticar(request, jwtService.verificarAccessToken(token));
        } catch (AutorizacionException ex) {
            SecurityContextHolder.clearContext();
            escritorDeErrorHttp.escribir(response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Deja al usuario autenticado en el contexto de la peticion. SecurityContextHolderFilter lo
     * limpia al terminar, asi que el hilo no arrastra al usuario a la peticion siguiente.
     */
    private void autenticar(HttpServletRequest request, UsuarioAutenticado usuario) {
        SecurityUser principal = new SecurityUser(usuario);

        UsernamePasswordAuthenticationToken autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
    }

    /** Devuelve el token de la cabecera, o null si no viene ninguna cabecera Bearer. */
    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA);

        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return null;
        }

        String token = cabecera.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
