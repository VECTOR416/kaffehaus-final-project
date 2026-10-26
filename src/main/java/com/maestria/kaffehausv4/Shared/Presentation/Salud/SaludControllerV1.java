package com.maestria.kaffehausv4.Shared.Presentation.Salud;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.Shared.Application.Salud.ConsultarSaludUseCase;
import com.maestria.kaffehausv4.Shared.Application.Salud.SaludResponse;

/**
 * Health check (GET api/v1/salud). No lleva @RequiereRol: SecurityConfig la deja publica para
 * que un balanceador, Docker o un monitor la consulten sin token. Responde 200 si el sistema
 * esta disponible y 503 si no, con el mismo cuerpo en ambos casos.
 */
@RestController
@RequestMapping("api/v1/salud")
public class SaludControllerV1 {

    private final ConsultarSaludUseCase consultarSaludUseCase;

    public SaludControllerV1(ConsultarSaludUseCase consultarSaludUseCase) {
        this.consultarSaludUseCase = consultarSaludUseCase;
    }

    @GetMapping
    public ResponseEntity<SaludResponse> consultar() {
        SaludResponse salud = consultarSaludUseCase.execute();
        HttpStatus estado = SaludResponse.DISPONIBLE.equals(salud.getEstado())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(estado).body(salud);
    }
}
