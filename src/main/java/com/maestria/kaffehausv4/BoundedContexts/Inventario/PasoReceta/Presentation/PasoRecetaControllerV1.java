package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Presentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ActualizarPasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.CrearPasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ObtenerPasosRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases.ActualizarPasoRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases.CrearPasoRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases.EliminarPasoRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases.ObtenerPasoRecetaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases.ObtenerPasosRecetaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/paso-receta")
public class PasoRecetaControllerV1 {

    private final CrearPasoRecetaUseCase crearPasoRecetaUseCase;
    private final ActualizarPasoRecetaUseCase actualizarPasoRecetaUseCase;
    private final ObtenerPasoRecetaPorIdUseCase obtenerPasoRecetaPorIdUseCase;
    private final ObtenerPasosRecetaUseCase obtenerPasosRecetaUseCase;
    private final EliminarPasoRecetaUseCase eliminarPasoRecetaUseCase;

    public PasoRecetaControllerV1(
            CrearPasoRecetaUseCase crearPasoRecetaUseCase,
            ActualizarPasoRecetaUseCase actualizarPasoRecetaUseCase,
            ObtenerPasoRecetaPorIdUseCase obtenerPasoRecetaPorIdUseCase,
            ObtenerPasosRecetaUseCase obtenerPasosRecetaUseCase,
            EliminarPasoRecetaUseCase eliminarPasoRecetaUseCase) {
        this.crearPasoRecetaUseCase = crearPasoRecetaUseCase;
        this.actualizarPasoRecetaUseCase = actualizarPasoRecetaUseCase;
        this.obtenerPasoRecetaPorIdUseCase = obtenerPasoRecetaPorIdUseCase;
        this.obtenerPasosRecetaUseCase = obtenerPasosRecetaUseCase;
        this.eliminarPasoRecetaUseCase = eliminarPasoRecetaUseCase;
    }

    @PostMapping
    public ResponseEntity<PasoRecetaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearPasoRecetaRequest request = CrearPasoRecetaRequest.createFromRequest(body);
        PasoRecetaResponse response = crearPasoRecetaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PasoRecetaResponse> obtenerPorId(@PathVariable String id) {
        PasoRecetaResponse response = obtenerPasoRecetaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<PasoRecetaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerPasosRecetaRequest request = ObtenerPasosRecetaRequest.createFromRequest(params);
        PaginatedResponse<List<PasoRecetaResponse>> response = obtenerPasosRecetaUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PasoRecetaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarPasoRecetaRequest request = ActualizarPasoRecetaRequest.createFromRequest(body);
        PasoRecetaResponse response = actualizarPasoRecetaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarPasoRecetaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
