package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ActualizarIngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.CrearIngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.IngredienteRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ObtenerIngredientesRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases.ActualizarIngredienteRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases.CrearIngredienteRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases.EliminarIngredienteRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases.ObtenerIngredienteRecetaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases.ObtenerIngredientesRecetaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/ingrediente-receta")
public class IngredienteRecetaControllerV1 {

    private final CrearIngredienteRecetaUseCase crearIngredienteRecetaUseCase;
    private final ActualizarIngredienteRecetaUseCase actualizarIngredienteRecetaUseCase;
    private final ObtenerIngredienteRecetaPorIdUseCase obtenerIngredienteRecetaPorIdUseCase;
    private final ObtenerIngredientesRecetaUseCase obtenerIngredientesRecetaUseCase;
    private final EliminarIngredienteRecetaUseCase eliminarIngredienteRecetaUseCase;

    public IngredienteRecetaControllerV1(
            CrearIngredienteRecetaUseCase crearIngredienteRecetaUseCase,
            ActualizarIngredienteRecetaUseCase actualizarIngredienteRecetaUseCase,
            ObtenerIngredienteRecetaPorIdUseCase obtenerIngredienteRecetaPorIdUseCase,
            ObtenerIngredientesRecetaUseCase obtenerIngredientesRecetaUseCase,
            EliminarIngredienteRecetaUseCase eliminarIngredienteRecetaUseCase) {
        this.crearIngredienteRecetaUseCase = crearIngredienteRecetaUseCase;
        this.actualizarIngredienteRecetaUseCase = actualizarIngredienteRecetaUseCase;
        this.obtenerIngredienteRecetaPorIdUseCase = obtenerIngredienteRecetaPorIdUseCase;
        this.obtenerIngredientesRecetaUseCase = obtenerIngredientesRecetaUseCase;
        this.eliminarIngredienteRecetaUseCase = eliminarIngredienteRecetaUseCase;
    }

    @PostMapping
    public ResponseEntity<IngredienteRecetaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearIngredienteRecetaRequest request = CrearIngredienteRecetaRequest.createFromRequest(body);
        IngredienteRecetaResponse response = crearIngredienteRecetaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredienteRecetaResponse> obtenerPorId(@PathVariable String id) {
        IngredienteRecetaResponse response = obtenerIngredienteRecetaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<IngredienteRecetaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerIngredientesRecetaRequest request = ObtenerIngredientesRecetaRequest.createFromRequest(params);
        PaginatedResponse<List<IngredienteRecetaResponse>> response = obtenerIngredientesRecetaUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredienteRecetaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarIngredienteRecetaRequest request = ActualizarIngredienteRecetaRequest.createFromRequest(body);
        IngredienteRecetaResponse response = actualizarIngredienteRecetaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarIngredienteRecetaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
