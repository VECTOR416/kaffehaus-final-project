package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.RolDelSistema;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web.ContextoDeSeguridad;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereAutenticacion;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web.AtributosDeSeguridad;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereRol;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.UsuarioDeLaPeticion;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller que existe solo para las pruebas del guard.
 *
 * Vive en src/test a proposito: permite verificar 401 y 403 sin cerrar ningun endpoint real de
 * la API, que es una decision de negocio que no corresponde a este modulo.
 */
@RestController
@RequestMapping("api/v1/pruebas-seguridad")
public class RecursoProtegidoDePruebaControllerV1 {

    /** Abierto: ningun endpoint sin anotar cambia de comportamiento. */
    @GetMapping("/abierto")
    public ResponseEntity<String> abierto() {
        return ResponseEntity.ok("abierto");
    }

    /** Cualquier usuario autenticado, sin importar el rol. */
    @RequiereAutenticacion
    @GetMapping("/autenticado")
    public ResponseEntity<String> autenticado() {
        return ResponseEntity.ok(ContextoDeSeguridad.exigirUsuarioActual().getUsuarioId());
    }

    /** Solo Administrador (rolId 2). */
    @RequiereRol(RolDelSistema.ADMINISTRADOR)
    @GetMapping("/solo-administrador")
    public ResponseEntity<String> soloAdministrador() {
        return ResponseEntity.ok(ContextoDeSeguridad.exigirUsuarioActual().getRolNombre());
    }

    /** Lee al usuario de los atributos de la peticion, al estilo req.user. */
    @RequiereAutenticacion
    @GetMapping("/atributos")
    public ResponseEntity<Map<String, Object>> atributosDeLaPeticion(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of(
                "usuarioId", request.getAttribute(AtributosDeSeguridad.USUARIO_ID),
                "rolId", request.getAttribute(AtributosDeSeguridad.ROL_ID),
                "rolNombre", request.getAttribute(AtributosDeSeguridad.ROL_NOMBRE),
                "esElMismoObjeto", request.getAttribute(AtributosDeSeguridad.USUARIO) instanceof UsuarioAutenticado));
    }

    /** Recibe al usuario inyectado como parametro, que es la via idiomatica en Spring MVC. */
    @RequiereAutenticacion
    @GetMapping("/inyectado")
    public ResponseEntity<Map<String, Object>> usuarioInyectado(@UsuarioDeLaPeticion UsuarioAutenticado usuario) {
        return ResponseEntity.ok(Map.of(
                "usuarioId", usuario.getUsuarioId(),
                "rolId", usuario.getRolId(),
                "nombreCompleto", usuario.getNombreCompleto()));
    }

    /** Endpoint abierto que se adapta: sabe quien llama solo si la peticion venia autenticada. */
    @GetMapping("/opcional")
    public ResponseEntity<String> usuarioOpcional(@UsuarioDeLaPeticion Optional<UsuarioAutenticado> usuario) {
        return ResponseEntity.ok(usuario.map(UsuarioAutenticado::getUsuarioId).orElse("anonimo"));
    }

    /** Solo Operador (rolId 1), para comprobar que la restriccion funciona en ambos sentidos. */
    @RequiereRol(RolDelSistema.OPERADOR)
    @GetMapping("/solo-operador")
    public ResponseEntity<String> soloOperador() {
        return ResponseEntity.ok(ContextoDeSeguridad.exigirUsuarioActual().getRolNombre());
    }
}
