package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

import org.springframework.context.annotation.Configuration;
import java.util.List;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el guard en la cadena de Spring MVC.
 *
 * Se declara aparte de Shared/Infrastructure/Web/WebConfig, que sigue ocupandose solo del
 * CORS: asi la configuracion de seguridad vive dentro de su bounded context y se puede leer,
 * mover o retirar entera.
 *
 * El interceptor se aplica a todas las rutas porque el filtrado real lo hacen las anotaciones:
 * sobre un handler sin @RequiereAutenticacion ni @RequiereRol no hace nada.
 */
@Configuration
public class SeguridadWebConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacionInterceptor;
    private final UsuarioDeLaPeticionArgumentResolver usuarioDeLaPeticionArgumentResolver;

    public SeguridadWebConfig(
            AutenticacionInterceptor autenticacionInterceptor,
            UsuarioDeLaPeticionArgumentResolver usuarioDeLaPeticionArgumentResolver) {
        this.autenticacionInterceptor = autenticacionInterceptor;
        this.usuarioDeLaPeticionArgumentResolver = usuarioDeLaPeticionArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacionInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(usuarioDeLaPeticionArgumentResolver);
    }
}
