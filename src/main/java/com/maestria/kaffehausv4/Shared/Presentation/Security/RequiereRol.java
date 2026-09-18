package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restringe un endpoint a los roles indicados.
 *
 * Los valores son nombres de la tabla 'rol', escritos tal cual estan en la base (la comparacion
 * ignora mayusculas). No hay ningun enum ni catalogo de roles en el codigo: el rol del usuario
 * se lee de la base al iniciar sesion, viaja firmado en el token y se compara aqui contra estos
 * nombres. Crear un rol nuevo es insertar una fila y nombrarlo en esta anotacion.
 *
 * Se puede poner sobre un metodo o sobre la clase entera del controller; la del metodo manda.
 *
 * <pre>
 * &#64;RequiereRol("Administrador")
 * &#64;PostMapping
 * public ResponseEntity&lt;X&gt; crear(&#64;AuthenticationPrincipal SecurityUser usuario) { ... }
 * </pre>
 *
 * Quien la aplica es GuardiaDeRoles. Un endpoint sin esta anotacion sigue exigiendo un token
 * valido (la cadena de seguridad cierra todo salvo /api/v1/auth); lo que la anotacion anade es
 * la exigencia de un rol concreto: 401 si no hay token, 403 si el rol no figura en la lista.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereRol {

    /** Nombres de rol que pueden entrar. Basta con tener uno de ellos. */
    String[] value();
}
