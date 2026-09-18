package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.RolDelSistema;

/**
 * Restringe un endpoint a los roles indicados.
 *
 * Implica autenticacion: sin token valido la respuesta es 401, y con token valido pero rol que
 * no figura en la lista, 403.
 *
 * Ejemplo: @RequiereRol(RolDelSistema.ADMINISTRADOR)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereRol {

    /** Roles que pueden entrar. Basta con tener uno de ellos. */
    RolDelSistema[] value();
}
