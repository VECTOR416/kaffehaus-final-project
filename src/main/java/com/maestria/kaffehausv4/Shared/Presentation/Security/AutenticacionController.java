package com.maestria.kaffehausv4.Shared.Presentation.Security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AccessTokenResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.RenovarTokenRequest;

/**
 * Endpoints de autenticacion.
 *
 * No sigue la plantilla de agregado del proyecto (nada de UseCases, puertos ni datasource
 * propio) porque la autenticacion no es un dominio de negocio: es un servicio tecnico de la
 * capa externa que reutiliza los repositorios que ya existen. Ver AutenticacionService.
 *
 * Los tres endpoints publicos devuelven 200 y no 201: iniciar sesion no crea ningun recurso.
 * Son los unicos que la cadena de seguridad deja abiertos; sin ellos no habria forma de
 * conseguir un token.
 */
@RestController
@RequestMapping(SecurityConfig.RUTA_DE_AUTENTICACION)
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public ResponseEntity<AutenticacionResponse> iniciarSesion(@RequestBody IniciarSesionRequest peticion) {
        return ResponseEntity.ok(autenticacionService.iniciarSesion(peticion));
    }

    @PostMapping("/google")
    public ResponseEntity<AutenticacionResponse> iniciarSesionConGoogle(
            @RequestBody IniciarSesionConGoogleRequest peticion) {
        return ResponseEntity.ok(autenticacionService.iniciarSesionConGoogle(peticion));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> renovar(@RequestBody RenovarTokenRequest peticion) {
        return ResponseEntity.ok(autenticacionService.renovarAccessToken(peticion));
    }
}
