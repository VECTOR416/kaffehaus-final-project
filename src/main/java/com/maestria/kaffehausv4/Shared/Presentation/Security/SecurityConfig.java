package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Toda la configuracion de seguridad del proyecto, en una sola clase.
 *
 * El esquema es deliberadamente corto y tiene dos piezas:
 *
 * 1. La cadena de filtros cierra el API: solo /api/v1/auth queda abierto (y /api/v1/seed
 *    mientras la siembra de primer arranque este habilitada), todo lo demas exige un access
 *    token valido. Quien lo valida es JwtAuthenticationFilter.
 * 2. Los roles se exigen con @RequiereRol, que aplica GuardiaDeRoles. Todo controller de un
 *    agregado lleva @RequiereRol({"Operador", "Administrador"}) a nivel de clase; un metodo
 *    puede restringirlo mas con su propia anotacion. No hay tabla de rutas que mantener en
 *    paralelo a los controllers.
 *
 * Y un complemento: @UsuarioEnSesion inyecta en el controller el id del usuario del token, que
 * resuelve ResolvedorDeUsuarioEnSesion. El frontend nunca envia 'idUsuario'.
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
     * Siembra de primer arranque (SembrarSistemaUseCase). En una base vacia no hay ningun
     * usuario que pueda pedir un token, asi que tiene que poder llamarse sin el. Con
     * 'seguridad.seed-habilitado' en false deja de ser publica y solo la alcanza un
     * Administrador: es idempotente, pero no tiene por que quedar abierta en produccion.
     */
    public static final String RUTA_DE_SIEMBRA = "/api/v1/seed";

    /**
     * Health check (SaludControllerV1). Publica para que la consulten un balanceador o un
     * monitor sin credenciales; solo el GET, y solo responde "UP" o "DOWN".
     */
    public static final String RUTA_DE_SALUD = "/api/v1/salud";

    /**
     * Lo unico que se inyecta por constructor es el guardia, porque hace falta para configurar
     * Spring MVC. El filtro y el escritor llegan como parametros del @Bean de abajo: asi no se
     * construyen hasta que se arma la cadena, y la configuracion de MVC no arrastra media
     * aplicacion al arrancar.
     */
    private final GuardiaDeRoles guardiaDeRoles;
    private final ResolvedorDeUsuarioEnSesion resolvedorDeUsuarioEnSesion;

    public SecurityConfig(GuardiaDeRoles guardiaDeRoles, ResolvedorDeUsuarioEnSesion resolvedorDeUsuarioEnSesion) {
        this.guardiaDeRoles = guardiaDeRoles;
        this.resolvedorDeUsuarioEnSesion = resolvedorDeUsuarioEnSesion;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            EscritorDeErrorHttp escritorDeErrorHttp,
            @Value("${seguridad.seed-habilitado:false}") boolean seedHabilitado) throws Exception {
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
                // Solo el login, los tokens, el health check y la siembra habilitada quedan abiertos;
                // todo lo demas exige un token valido. SecurityUser concede ROLE_ADMINISTRADOR al rol
                // 'Administrador'.
                .authorizeHttpRequests(rutas -> {
                    rutas.requestMatchers(RUTA_DE_AUTENTICACION + "/**").permitAll();
                    rutas.requestMatchers(HttpMethod.GET, RUTA_DE_SALUD).permitAll();
                    if (seedHabilitado) {
                        rutas.requestMatchers(RUTA_DE_SIEMBRA).permitAll();
                    } else {
                        rutas.requestMatchers(RUTA_DE_SIEMBRA).hasRole("ADMINISTRADOR");
                    }
                    rutas.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Registra el guardia de @RequiereRol. Sobre un endpoint sin anotar no hace nada. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guardiaDeRoles).addPathPatterns("/**");
    }

    /** Registra el resolvedor de @UsuarioEnSesion. */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolvedorDeUsuarioEnSesion);
    }

    /**
     * CORS de la API.
     *
     * Se declara dentro de la cadena de seguridad, y no como WebMvcConfigurer, porque los
     * filtros corren antes que Spring MVC: si el preflight OPTIONS no se resuelve aqui, el
     * navegador recibe un 401 antes de que MVC llegue a opinar.
     *
     * Los origenes se enumeran en 'seguridad.cors.origenes-permitidos' (CORS_ORIGENES), sin
     * valor por defecto. Aunque la autenticacion viaja en la cabecera
     * Authorization y no en cookies, abrir la API a cualquier sitio no aporta nada, asi que el
     * comodin '*' a secas se rechaza al arrancar. Se admiten patrones acotados, como un puerto
     * cualquiera de localhost ('http://localhost:[*]') o los subdominios de un dominio propio.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${seguridad.cors.origenes-permitidos}") String[] origenesPermitidos) {
        List<String> origenes = Arrays.stream(origenesPermitidos)
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
        if (origenes.isEmpty() || origenes.contains("*")) {
            throw new IllegalStateException(
                    "seguridad.cors.origenes-permitidos (CORS_ORIGENES) debe enumerar los origenes del frontend; el comodin '*' no esta permitido");
        }

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(origenes);
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
