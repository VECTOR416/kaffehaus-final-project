package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ActualizarCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CrearCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.ActualizarCategoriaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.CrearCategoriaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.EliminarCategoriaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.ObtenerCategoriaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.ObtenerCategoriasUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/categoria")
public class CategoriaControllerV1 {

    private final CrearCategoriaUseCase crearCategoriaUseCase;
    private final ActualizarCategoriaUseCase actualizarCategoriaUseCase;
    private final ObtenerCategoriaPorIdUseCase obtenerCategoriaPorIdUseCase;
    private final ObtenerCategoriasUseCase obtenerCategoriasUseCase;
    private final EliminarCategoriaUseCase eliminarCategoriaUseCase;

    public CategoriaControllerV1(
            CrearCategoriaUseCase crearCategoriaUseCase,
            ActualizarCategoriaUseCase actualizarCategoriaUseCase,
            ObtenerCategoriaPorIdUseCase obtenerCategoriaPorIdUseCase,
            ObtenerCategoriasUseCase obtenerCategoriasUseCase,
            EliminarCategoriaUseCase eliminarCategoriaUseCase) {
        this.crearCategoriaUseCase = crearCategoriaUseCase;
        this.actualizarCategoriaUseCase = actualizarCategoriaUseCase;
        this.obtenerCategoriaPorIdUseCase = obtenerCategoriaPorIdUseCase;
        this.obtenerCategoriasUseCase = obtenerCategoriasUseCase;
        this.eliminarCategoriaUseCase = eliminarCategoriaUseCase;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearCategoriaRequest request = CrearCategoriaRequest.createFromRequest(body);
        CategoriaResponse response = crearCategoriaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerPorId(@PathVariable String id) {
        CategoriaResponse response = obtenerCategoriaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<CategoriaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerCategoriasRequest request = ObtenerCategoriasRequest.createFromRequest(params);
        PaginatedResponse<List<CategoriaResponse>> response = obtenerCategoriasUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarCategoriaRequest request = ActualizarCategoriaRequest.createFromRequest(body);
        CategoriaResponse response = actualizarCategoriaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarCategoriaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
