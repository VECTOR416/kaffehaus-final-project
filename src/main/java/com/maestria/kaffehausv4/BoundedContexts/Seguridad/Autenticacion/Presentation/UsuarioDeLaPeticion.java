package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parametro del controller al usuario que firma la peticion.
 *
 * Es el equivalente tipado del req.user de otros entornos:
 *
 * <pre>
 * &#64;RequiereRol(RolDelSistema.ADMINISTRADOR)
 * &#64;PostMapping
 * public ResponseEntity&lt;X&gt; crear(&#64;UsuarioDeLaPeticion UsuarioAutenticado usuario,
 *                                &#64;RequestBody Map&lt;String, Object&gt; body) {
 *     usuario.getUsuarioId();
 *     usuario.getRolId();
 * }
 * </pre>
 *
 * Declarar el parametro significa que el endpoint necesita saber quien llama, asi que si la
 * peticion no esta autenticada la respuesta es 401. Para un endpoint que funcione con o sin
 * usuario, declararlo como Optional&lt;UsuarioAutenticado&gt;.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioDeLaPeticion {
}
