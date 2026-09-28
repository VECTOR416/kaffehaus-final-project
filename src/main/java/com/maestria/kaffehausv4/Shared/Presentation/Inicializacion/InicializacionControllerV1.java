package com.maestria.kaffehausv4.Shared.Presentation.Inicializacion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.Shared.Application.Inicializacion.SembrarSistemaResponse;
import com.maestria.kaffehausv4.Shared.Application.Inicializacion.SembrarSistemaUseCase;

/**
 * Siembra de primer arranque (POST api/v1/seed). No lleva @RequiereRol: quien decide si la
 * ruta es publica es SecurityConfig, segun 'inicializacion.seed-habilitado'. Habilitada, es
 * publica, porque en una base vacia todavia no existe nadie que pueda autenticarse; deshabilitada,
 * solo la alcanza un Administrador. Responde 200 aunque siembre filas: es idempotente y la
 * misma llamada repetida no crea nada.
 */
@RestController
@RequestMapping("api/v1/seed")
public class InicializacionControllerV1 {

    private final SembrarSistemaUseCase sembrarSistemaUseCase;

    public InicializacionControllerV1(SembrarSistemaUseCase sembrarSistemaUseCase) {
        this.sembrarSistemaUseCase = sembrarSistemaUseCase;
    }

    @PostMapping
    public ResponseEntity<SembrarSistemaResponse> sembrar() {
        return ResponseEntity.ok(sembrarSistemaUseCase.execute());
    }
}
