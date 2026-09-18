package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.RolDelSistema;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web.ContextoDeSeguridad;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereAutenticacion;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation.RequiereRol;

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

    /** Solo Operador (rolId 1), para comprobar que la restriccion funciona en ambos sentidos. */
    @RequiereRol(RolDelSistema.OPERADOR)
    @GetMapping("/solo-operador")
    public ResponseEntity<String> soloOperador() {
        return ResponseEntity.ok(ContextoDeSeguridad.exigirUsuarioActual().getRolNombre());
    }
}
