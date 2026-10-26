package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ActualizarDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.CrearDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases.ActualizarDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases.CrearDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases.EliminarDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases.ObtenerDescuentoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases.ObtenerDescuentosUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/descuento")
public class DescuentoControllerV1 {

    private final CrearDescuentoUseCase crearDescuentoUseCase;
    private final ActualizarDescuentoUseCase actualizarDescuentoUseCase;
    private final ObtenerDescuentoPorIdUseCase obtenerDescuentoPorIdUseCase;
    private final ObtenerDescuentosUseCase obtenerDescuentosUseCase;
    private final EliminarDescuentoUseCase eliminarDescuentoUseCase;

    public DescuentoControllerV1(
            CrearDescuentoUseCase crearDescuentoUseCase,
            ActualizarDescuentoUseCase actualizarDescuentoUseCase,
            ObtenerDescuentoPorIdUseCase obtenerDescuentoPorIdUseCase,
            ObtenerDescuentosUseCase obtenerDescuentosUseCase,
            EliminarDescuentoUseCase eliminarDescuentoUseCase) {
        this.crearDescuentoUseCase = crearDescuentoUseCase;
        this.actualizarDescuentoUseCase = actualizarDescuentoUseCase;
        this.obtenerDescuentoPorIdUseCase = obtenerDescuentoPorIdUseCase;
        this.obtenerDescuentosUseCase = obtenerDescuentosUseCase;
        this.eliminarDescuentoUseCase = eliminarDescuentoUseCase;
    }

    @PostMapping
    public ResponseEntity<DescuentoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearDescuentoRequest request = CrearDescuentoRequest.createFromRequest(body);
        DescuentoResponse response = crearDescuentoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DescuentoResponse> obtenerPorId(@PathVariable String id) {
        DescuentoResponse response = obtenerDescuentoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<DescuentoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerDescuentosRequest request = ObtenerDescuentosRequest.createFromRequest(params);
        PaginatedResponse<List<DescuentoResponse>> response = obtenerDescuentosUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DescuentoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarDescuentoRequest request = ActualizarDescuentoRequest.createFromRequest(body);
        DescuentoResponse response = actualizarDescuentoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarDescuentoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
