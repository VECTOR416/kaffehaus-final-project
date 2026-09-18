package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller que existe solo para las pruebas de la cadena de seguridad.
 *
 * Vive en src/test a proposito: permite verificar 401, 403 y la inyeccion del principal sin
 * depender de endpoints reales, que ademas tocarian la base de datos.
 *
 * Su ruta no figura en RutasDeSeguridad, asi que cae en 'anyRequest().authenticated()': sirve
 * ademas para comprobar que un controller nuevo nace protegido.
 */
@RestController
@RequestMapping("api/v1/pruebas-seguridad")
public class RecursoProtegidoDePruebaControllerV1 {

    /** Cualquier usuario autenticado, sin importar el rol. */
    @GetMapping("/autenticado")
    public ResponseEntity<String> autenticado(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getUsuarioId());
    }

    /** El usuario inyectado como parametro: la via idiomatica en Spring Security. */
    @GetMapping("/inyectado")
    public ResponseEntity<Map<String, Object>> usuarioInyectado(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(Map.of(
                "usuarioId", usuario.getUsuarioId(),
                "rolId", usuario.getRolId(),
                "rolNombre", usuario.getRolNombre(),
                "nombreCompleto", usuario.getNombreCompleto()));
    }

    /** Las autoridades concedidas, para comprobar como se traduce el rol de la tabla. */
    @GetMapping("/autoridades")
    public ResponseEntity<List<String>> autoridades(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
    }

    /** Restriccion por metodo, no por ruta: solo Administrador. */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/solo-administrador")
    public ResponseEntity<String> soloAdministrador(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getRolNombre());
    }

    /** Solo Operador, para comprobar que la restriccion funciona en ambos sentidos. */
    @PreAuthorize("hasRole('OPERADOR')")
    @GetMapping("/solo-operador")
    public ResponseEntity<String> soloOperador(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getRolNombre());
    }
}
