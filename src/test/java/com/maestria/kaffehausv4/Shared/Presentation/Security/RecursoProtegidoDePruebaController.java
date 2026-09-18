package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller que existe solo para las pruebas de la seguridad.
 *
 * Vive en src/test a proposito: permite verificar 401, 403 y la inyeccion del usuario sin
 * depender de endpoints reales, que ademas tocarian la base de datos.
 *
 * Los nombres de rol son los mismos que siembra la prueba en la tabla 'rol'.
 */
@RestController
@RequestMapping("api/v1/pruebas-seguridad")
public class RecursoProtegidoDePruebaController {

    /** Sin @RequiereRol: basta con estar autenticado. */
    @GetMapping("/autenticado")
    public ResponseEntity<String> autenticado(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getUsuarioId());
    }

    /** El usuario inyectado como parametro: la via idiomatica en Spring Security. */
    @GetMapping("/inyectado")
    public ResponseEntity<Map<String, Object>> usuarioInyectado(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(Map.of(
                "usuarioId", usuario.getUsuarioId(),
                "nombreCompleto", usuario.getNombreCompleto(),
                "rolId", usuario.getRolId(),
                "rolNombre", usuario.getRolNombre(),
                "autoridad", usuario.getAuthorities().iterator().next().getAuthority()));
    }

    @RequiereRol("Administrador")
    @GetMapping("/solo-administrador")
    public ResponseEntity<String> soloAdministrador(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getRolNombre());
    }

    @RequiereRol("Operador")
    @GetMapping("/solo-operador")
    public ResponseEntity<String> soloOperador(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getRolNombre());
    }

    /** Varios roles admitidos: basta con tener uno. */
    @RequiereRol({"Administrador", "Operador"})
    @GetMapping("/cualquiera-de-los-dos")
    public ResponseEntity<String> cualquieraDeLosDos(@AuthenticationPrincipal SecurityUser usuario) {
        return ResponseEntity.ok(usuario.getRolNombre());
    }
}
