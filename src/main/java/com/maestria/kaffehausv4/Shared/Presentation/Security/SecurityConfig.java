package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Toda la configuracion de seguridad del proyecto, en una sola clase.
 *
 * El esquema es deliberadamente corto y tiene dos piezas:
 *
 * 1. La cadena de filtros cierra el API: solo /api/v1/auth queda abierto, todo lo demas exige
 *    un access token valido. Quien lo valida es JwtAuthenticationFilter.
 * 2. Los roles se exigen endpoint por endpoint con @RequiereRol, que aplica GuardiaDeRoles.
 *    No hay tabla de rutas que mantener en paralelo a los controllers.
 *
 * Politica de la cadena:
 *
 * - Stateless y sin CSRF. No hay sesion de servidor ni cookies de autenticacion: cada peticion
 *   se acredita con su Bearer token, y sin cookies el ataque que CSRF previene no es posible.
 * - Sin formLogin, httpBasic ni logout. El login es /api/v1/auth/login; no queremos las
 *   pantallas ni los flujos que Spring Security monta por defecto.
 * - Cerrado por defecto: anyRequest().authenticated(). Un controller nuevo nace protegido.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    /** Lo unico publico: sin estos endpoints no hay forma de conseguir un token. */
    public static final String RUTA_DE_AUTENTICACION = "/api/v1/auth";

    /**
     * Lo unico que se inyecta por constructor es el guardia, porque hace falta para configurar
     * Spring MVC. El filtro y el escritor llegan como parametros del @Bean de abajo: asi no se
     * construyen hasta que se arma la cadena, y la configuracion de MVC no arrastra media
     * aplicacion al arrancar.
     */
    private final GuardiaDeRoles guardiaDeRoles;

    public SecurityConfig(GuardiaDeRoles guardiaDeRoles) {
        this.guardiaDeRoles = guardiaDeRoles;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            EscritorDeErrorHttp escritorDeErrorHttp) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 en JSON en vez de la redireccion a un formulario que aqui no existe.
                .exceptionHandling(errores -> errores.authenticationEntryPoint(
                        (peticion, respuesta, excepcion) -> escritorDeErrorHttp.escribir(respuesta,
                                new AutorizacionException(401,
                                        "Falta la cabecera Authorization con un token Bearer valido"))))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTA_DE_AUTENTICACION + "/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Registra el guardia de @RequiereRol. Sobre un endpoint sin anotar no hace nada. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guardiaDeRoles).addPathPatterns("/**");
    }

    /**
     * CORS de la API.
     *
     * Se declara dentro de la cadena de seguridad, y no como WebMvcConfigurer, porque los
     * filtros corren antes que Spring MVC: si el preflight OPTIONS no se resuelve aqui, el
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
     * Spring Boot registra automaticamente todo bean de tipo Filter en la cadena del servlet. El
     * filtro ya entra donde debe, dentro de la cadena de seguridad, asi que esa segunda
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
     * credenciales las comprueba AutenticacionService y lo que circula despues es un token. Un
     * usuario de respaldo con contrasena aleatoria solo seria una puerta que nadie vigila.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return usuario -> {
            throw new UsernameNotFoundException(
                    "La autenticacion por usuario y contrasena no esta habilitada: use " + RUTA_DE_AUTENTICACION + "/login");
        };
    }
}
