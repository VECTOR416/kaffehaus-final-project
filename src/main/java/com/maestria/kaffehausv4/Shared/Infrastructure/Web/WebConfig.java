package com.maestria.kaffehausv4.Shared.Infrastructure.Web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion global de MVC.
 *
 * El CORS lo declaraba antes la cadena de filtros de Spring Security; al retirarla,
 * pasa a configurarse aqui, a nivel de WebMvcConfigurer, que es el mecanismo del
 * propio Spring MVC y no depende de ninguna libreria de seguridad.
 *
 * La API esta abierta y no usa cookies ni cabecera Authorization, asi que se permite
 * cualquier origen. Cuando se implemente el nuevo esquema de autenticacion habra que
 * restringir allowedOriginPatterns a los dominios del frontend, porque en cuanto se
 * envien credenciales el comodin deja de ser aceptable para el navegador.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
