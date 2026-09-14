package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.ActualizarRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.CrearRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.EliminarRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.ObtenerRecetaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.ObtenerRecetasUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/receta")
public class RecetaControllerV1 {

    private final CrearRecetaUseCase crearRecetaUseCase;
    private final ActualizarRecetaUseCase actualizarRecetaUseCase;
    private final ObtenerRecetaPorIdUseCase obtenerRecetaPorIdUseCase;
    private final ObtenerRecetasUseCase obtenerRecetasUseCase;
    private final EliminarRecetaUseCase eliminarRecetaUseCase;

    public RecetaControllerV1(
            CrearRecetaUseCase crearRecetaUseCase,
            ActualizarRecetaUseCase actualizarRecetaUseCase,
            ObtenerRecetaPorIdUseCase obtenerRecetaPorIdUseCase,
            ObtenerRecetasUseCase obtenerRecetasUseCase,
            EliminarRecetaUseCase eliminarRecetaUseCase) {
        this.crearRecetaUseCase = crearRecetaUseCase;
        this.actualizarRecetaUseCase = actualizarRecetaUseCase;
        this.obtenerRecetaPorIdUseCase = obtenerRecetaPorIdUseCase;
        this.obtenerRecetasUseCase = obtenerRecetasUseCase;
        this.eliminarRecetaUseCase = eliminarRecetaUseCase;
    }

    @PostMapping
    public ResponseEntity<RecetaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearRecetaRequest request = CrearRecetaRequest.createFromRequest(body);
        RecetaResponse response = crearRecetaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecetaResponse> obtenerPorId(@PathVariable String id) {
        RecetaResponse response = obtenerRecetaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<RecetaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerRecetasRequest request = ObtenerRecetasRequest.createFromRequest(params);
        PaginatedResponse<List<RecetaResponse>> response = obtenerRecetasUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecetaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarRecetaRequest request = ActualizarRecetaRequest.createFromRequest(body);
        RecetaResponse response = actualizarRecetaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarRecetaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
