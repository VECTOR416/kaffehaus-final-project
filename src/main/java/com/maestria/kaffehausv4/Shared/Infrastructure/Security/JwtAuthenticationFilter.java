package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticacion por Bearer token.
 *
 * Unica responsabilidad: si la peticion trae 'Authorization: Bearer <token>', verificarlo y
 * dejar al usuario en el SecurityContext. No decide quien puede entrar a donde; de eso se
 * ocupan las reglas de SecurityConfig y, si hace falta grano fino, @PreAuthorize.
 *
 * Extiende OncePerRequestFilter para garantizar una sola ejecucion por peticion aunque el
 * contenedor lo registre en mas de un punto (forwards, error dispatches, etc.).
 *
 * Tres comportamientos deliberados:
 *
 * - Sin cabecera Authorization, la peticion sigue su curso sin autenticar. Quien la rechaza,
 *   si la ruta lo exige, es el motor de autorizacion a traves de JwtAuthenticationEntryPoint,
 *   que es quien sabe si esa ruta era publica o no.
 * - Con un token presente pero invalido o caducado, se responde 401 aqui mismo y se corta la
 *   cadena. Ignorarlo en silencio le escondaria al cliente que su sesion expiro.
 * - Las rutas de autenticacion se saltan el filtro entero (shouldNotFilter): pedir un token
 *   nuevo no puede fallar por culpa del token viejo que el cliente aun tenga guardado.
 *
 * No escribe el error a mano: delega en EscritorDeErrorHttp para que un 401 de aqui tenga
 * exactamente la misma forma que cualquier otro error del sistema.
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
        return RutasDeSeguridad.esPublica(request.getRequestURI());
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
