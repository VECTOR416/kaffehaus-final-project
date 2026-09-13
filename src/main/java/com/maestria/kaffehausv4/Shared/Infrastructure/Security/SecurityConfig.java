package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;
import com.maestria.kaffehausv4.Shared.Presentation.Security.JwtAccessDeniedHandler;
import com.maestria.kaffehausv4.Shared.Presentation.Security.JwtAuthenticationEntryPoint;

/**
 * Cadena de filtros de seguridad: unico lugar del proyecto donde se declara QUIEN
 * puede hacer QUE.
 *
 * Los controladores no se tocan. La validacion del token y la comprobacion de rol
 * ocurren en filtros de servlet, antes del DispatcherServlet, asi que cuando la
 * peticion llega a EntidadControllerV1 ya esta autorizada y el controlador sigue
 * haciendo exactamente lo mismo que hacia antes.
 *
 * Las reglas aprovechan que el proyecto mantiene una convencion de rutas regular
 * (api/v1/entidad, api/v1/cliente, api/v1/proveedor, ...), de modo que un contexto
 * nuevo queda protegido sin editar nada aqui.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(
            KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API REST consumida por token: sin sesion y sin CSRF.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                // Se enumeran los roles explicitamente en lugar de usar una jerarquia:
                // con hasRole("EMPLEADO") un ADMINISTRADOR que no tuviera tambien el rol
                // EMPLEADO recibiria 403 en los GET.
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(
                        RolDeSistema.ADMINISTRADOR.name(),
                        RolDeSistema.SUPERVISOR.name(),
                        RolDeSistema.EMPLEADO.name(),
                        RolDeSistema.CAJERO.name())

                .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole(
                        RolDeSistema.ADMINISTRADOR.name(),
                        RolDeSistema.SUPERVISOR.name())

                .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole(
                        RolDeSistema.ADMINISTRADOR.name(),
                        RolDeSistema.SUPERVISOR.name())

                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole(
                        RolDeSistema.ADMINISTRADOR.name())

                .anyRequest().authenticated())

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))

            // Sin esto, los 401/403 saldrian con un formato distinto al del resto de la API.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler));

        return http.build();
    }

    /**
     * TRANSITORIO. CrearUsuarioUseCase y ActualizarUsuarioUseCase reciben hoy un
     * PasswordEncoder por constructor, pero el proyecto no declaraba ningun bean que
     * lo proveyera, de modo que el contexto de Spring no podia arrancar.
     *
     * Se declara aqui para desbloquear el arranque sin modificar esos casos de uso,
     * que quedan fuera del alcance de este paso. Debe ELIMINARSE junto con el campo
     * 'contrasena' del agregado Usuario y la columna 'usuario.contrasena' cuando se
     * complete la fase de sincronizacion: a partir de ahi las credenciales son
     * responsabilidad exclusiva de Keycloak y el backend no debe hashear nada.
     */
    @Bean
    @Deprecated
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
