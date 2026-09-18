package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige un access token valido para llegar al endpoint, sin importar el rol.
 *
 * Se puede poner en un metodo o en la clase entera del controller. La proteccion es opt-in:
 * un endpoint sin esta anotacion ni @RequiereRol sigue abierto, que es como esta hoy el resto
 * de la API. Cerrar todo por defecto es cambiar una linea en AutenticacionInterceptor.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereAutenticacion {
}
