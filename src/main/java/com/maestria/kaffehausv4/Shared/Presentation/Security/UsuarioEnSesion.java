package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parametro String del controller el id del usuario que firma la peticion.
 *
 * El id sale del access token que valido JwtAuthenticationFilter, nunca del cuerpo ni de la
 * URL: el frontend no envia 'idUsuario' en ninguna operacion, y aunque lo enviara no se usaria.
 * Asi nadie puede registrar una venta, una apertura o una anulacion a nombre de otro.
 *
 * Existe en lugar de @AuthenticationPrincipal para que los controllers de los agregados no
 * importen org.springframework.security (arquitectura.md, regla 10): igual que @RequiereRol,
 * es una pieza de este paquete que los controllers usan sin conocer el framework. La resuelve
 * ResolvedorDeUsuarioEnSesion.
 *
 * <pre>
 * &#64;PostMapping
 * public ResponseEntity&lt;X&gt; crear(&#64;RequestBody Map&lt;String, Object&gt; body, &#64;UsuarioEnSesion String idUsuario) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioEnSesion {
}
