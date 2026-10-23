package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ActualizarUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.CrearUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases.ActualizarUnidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases.CrearUnidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases.EliminarUnidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases.ObtenerUnidadPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases.ObtenerUnidadesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/unidad")
public class UnidadControllerV1 {

    private final CrearUnidadUseCase crearUnidadUseCase;
    private final ActualizarUnidadUseCase actualizarUnidadUseCase;
    private final ObtenerUnidadPorIdUseCase obtenerUnidadPorIdUseCase;
    private final ObtenerUnidadesUseCase obtenerUnidadesUseCase;
    private final EliminarUnidadUseCase eliminarUnidadUseCase;

    public UnidadControllerV1(
            CrearUnidadUseCase crearUnidadUseCase,
            ActualizarUnidadUseCase actualizarUnidadUseCase,
            ObtenerUnidadPorIdUseCase obtenerUnidadPorIdUseCase,
            ObtenerUnidadesUseCase obtenerUnidadesUseCase,
            EliminarUnidadUseCase eliminarUnidadUseCase) {
        this.crearUnidadUseCase = crearUnidadUseCase;
        this.actualizarUnidadUseCase = actualizarUnidadUseCase;
        this.obtenerUnidadPorIdUseCase = obtenerUnidadPorIdUseCase;
        this.obtenerUnidadesUseCase = obtenerUnidadesUseCase;
        this.eliminarUnidadUseCase = eliminarUnidadUseCase;
    }

    @PostMapping
    public ResponseEntity<UnidadResponse> crear(@RequestBody Map<String, Object> body) {
        CrearUnidadRequest request = CrearUnidadRequest.createFromRequest(body);
        UnidadResponse response = crearUnidadUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadResponse> obtenerPorId(@PathVariable String id) {
        UnidadResponse response = obtenerUnidadPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<UnidadResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerUnidadesRequest request = ObtenerUnidadesRequest.createFromRequest(params);
        PaginatedResponse<List<UnidadResponse>> response = obtenerUnidadesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarUnidadRequest request = ActualizarUnidadRequest.createFromRequest(body);
        UnidadResponse response = actualizarUnidadUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    // Solo un Administrador da de baja recetas y filas de catalogo (logica.md GENERAL-17).
    @RequiereRol("Administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarUnidadUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
