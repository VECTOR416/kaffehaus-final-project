package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;

/**
 * Configuracion de la cadena de filtros de seguridad. Es el borde exterior de la aplicacion:
 * ninguna capa de negocio sabe que esta clase existe.
 *
 * Politica general:
 *
 * - Stateless y sin CSRF. No hay sesion de servidor ni cookies de autenticacion: cada peticion
 *   se acredita por si misma con su Bearer token, y sin cookies el ataque que CSRF previene no
 *   es posible. Dejar CSRF activo solo romperia a los clientes.
 * - Sin formLogin, httpBasic ni logout. El login es un endpoint propio (/api/v1/auth/login) que
 *   resuelve un caso de uso; no queremos las pantallas ni los flujos que Spring Security monta
 *   por defecto.
 * - Cerrado por defecto: anyRequest().authenticated(). Un controller nuevo nace protegido, que
 *   es justo lo contrario de lo que pasaba con el esquema anterior de anotaciones opt-in.
 *
 * Las rutas concretas de cada grupo estan en RutasDeSeguridad. Los roles no se escriben a mano:
 * salen de RolDelSistema, que a su vez se resuelve contra la tabla 'rol' en cada login.
 *
 * Para reglas mas finas que una ruta (por ejemplo, que un operador solo anule sus propias
 * ventas), @EnableMethodSecurity habilita @PreAuthorize en los controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMINISTRADOR = RolDelSistema.ADMINISTRADOR.getNombreDeAutoridad();
    private static final String OPERADOR = RolDelSistema.OPERADOR.getNombreDeAutoridad();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(rutas -> rutas
                        // Publicas: sin ellas no hay forma de conseguir un token.
                        .requestMatchers(RutasDeSeguridad.publicas()).permitAll()

                        // Administracion y politica comercial: solo Administrador.
                        .requestMatchers(RutasDeSeguridad.administracion()).hasRole(ADMINISTRADOR)
                        .requestMatchers(RutasDeSeguridad.politicaComercial()).hasRole(ADMINISTRADOR)

                        // Catalogos maestros: cualquiera los consulta, solo Administrador los toca.
                        .requestMatchers(HttpMethod.POST, RutasDeSeguridad.catalogosMaestros()).hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, RutasDeSeguridad.catalogosMaestros()).hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PATCH, RutasDeSeguridad.catalogosMaestros()).hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.DELETE, RutasDeSeguridad.catalogosMaestros()).hasRole(ADMINISTRADOR)

                        // Mostrador: la operacion diaria de ventas y pedidos.
                        .requestMatchers(RutasDeSeguridad.ventas()).hasAnyRole(ADMINISTRADOR, OPERADOR)

                        // Todo lo demas (inventario, produccion, lotes...): token valido.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CORS de la API.
     *
     * Vuelve a declararse dentro de la cadena de seguridad, y no como WebMvcConfigurer, porque
     * los filtros corren antes que Spring MVC: si el preflight OPTIONS no se resuelve aqui, el
     * navegador recibe un 401 antes de que MVC llegue a opinar.
     *
     * allowCredentials sigue en false y por eso el comodin de origen es aceptable: la
     * autenticacion viaja en la cabecera Authorization, no en cookies. Si algun dia se pasa a
     * cookies, hay que enumerar los dominios del frontend en allowedOriginPatterns.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(List.of("*"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    /**
     * Impide que el contenedor registre el filtro de JWT una segunda vez por su cuenta.
     *
     * Spring Boot registra automaticamente todo bean de tipo Filter en la cadena del servlet.
     * El filtro ya entra donde debe, dentro de la cadena de seguridad, asi que esa segunda
     * inscripcion solo lo haria correr fuera de contexto.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> registroDelFiltroJwt(JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /**
     * Desactiva el usuario en memoria que Spring Boot genera cuando no encuentra ningun
     * UserDetailsService (el del 'Using generated security password' en el arranque).
     *
     * Este API no autentica por usuario y contrasena a traves de Spring Security: las
     * credenciales las comprueba IniciarSesionUseCase y lo que circula despues es un token. Un
     * usuario de respaldo con contrasena aleatoria solo seria una puerta que nadie vigila.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return usuario -> {
            throw new UsernameNotFoundException(
                    "La autenticacion por usuario y contrasena no esta habilitada: use /api/v1/auth/login");
        };
    }
}
