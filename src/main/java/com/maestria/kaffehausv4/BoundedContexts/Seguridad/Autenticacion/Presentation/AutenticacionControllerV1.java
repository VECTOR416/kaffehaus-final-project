package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Presentation;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.AccessTokenResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.RenovarTokenRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.IniciarSesionConGoogleUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.IniciarSesionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.RenovarAccessTokenUseCase;

/**
 * Endpoints de autenticacion. Son los unicos que deben quedar siempre abiertos: sin ellos no
 * hay forma de obtener un token.
 *
 * Los tres devuelven 200 y no 201: iniciar sesion no crea ningun recurso.
 */
@RestController
@RequestMapping("api/v1/auth")
public class AutenticacionControllerV1 {

    private final IniciarSesionUseCase iniciarSesionUseCase;
    private final IniciarSesionConGoogleUseCase iniciarSesionConGoogleUseCase;
    private final RenovarAccessTokenUseCase renovarAccessTokenUseCase;

    public AutenticacionControllerV1(
            IniciarSesionUseCase iniciarSesionUseCase,
            IniciarSesionConGoogleUseCase iniciarSesionConGoogleUseCase,
            RenovarAccessTokenUseCase renovarAccessTokenUseCase) {
        this.iniciarSesionUseCase = iniciarSesionUseCase;
        this.iniciarSesionConGoogleUseCase = iniciarSesionConGoogleUseCase;
        this.renovarAccessTokenUseCase = renovarAccessTokenUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<AutenticacionResponse> iniciarSesion(@RequestBody Map<String, Object> body) {
        IniciarSesionRequest request = IniciarSesionRequest.createFromRequest(body);
        AutenticacionResponse response = iniciarSesionUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AutenticacionResponse> iniciarSesionConGoogle(@RequestBody Map<String, Object> body) {
        IniciarSesionConGoogleRequest request = IniciarSesionConGoogleRequest.createFromRequest(body);
        AutenticacionResponse response = iniciarSesionConGoogleUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> renovar(@RequestBody Map<String, Object> body) {
        RenovarTokenRequest request = RenovarTokenRequest.createFromRequest(body);
        AccessTokenResponse response = renovarAccessTokenUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
