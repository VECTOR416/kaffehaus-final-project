package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ActualizarTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.CrearTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases.ActualizarTipoMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases.CrearTipoMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases.EliminarTipoMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases.ObtenerTipoMovimientoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases.ObtenerTiposMovimientoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/tipo-movimiento")
public class TipoMovimientoControllerV1 {

    private final CrearTipoMovimientoUseCase crearTipoMovimientoUseCase;
    private final ActualizarTipoMovimientoUseCase actualizarTipoMovimientoUseCase;
    private final ObtenerTipoMovimientoPorIdUseCase obtenerTipoMovimientoPorIdUseCase;
    private final ObtenerTiposMovimientoUseCase obtenerTiposMovimientoUseCase;
    private final EliminarTipoMovimientoUseCase eliminarTipoMovimientoUseCase;

    public TipoMovimientoControllerV1(
            CrearTipoMovimientoUseCase crearTipoMovimientoUseCase,
            ActualizarTipoMovimientoUseCase actualizarTipoMovimientoUseCase,
            ObtenerTipoMovimientoPorIdUseCase obtenerTipoMovimientoPorIdUseCase,
            ObtenerTiposMovimientoUseCase obtenerTiposMovimientoUseCase,
            EliminarTipoMovimientoUseCase eliminarTipoMovimientoUseCase) {
        this.crearTipoMovimientoUseCase = crearTipoMovimientoUseCase;
        this.actualizarTipoMovimientoUseCase = actualizarTipoMovimientoUseCase;
        this.obtenerTipoMovimientoPorIdUseCase = obtenerTipoMovimientoPorIdUseCase;
        this.obtenerTiposMovimientoUseCase = obtenerTiposMovimientoUseCase;
        this.eliminarTipoMovimientoUseCase = eliminarTipoMovimientoUseCase;
    }

    @PostMapping
    public ResponseEntity<TipoMovimientoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearTipoMovimientoRequest request = CrearTipoMovimientoRequest.createFromRequest(body);
        TipoMovimientoResponse response = crearTipoMovimientoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoMovimientoResponse> obtenerPorId(@PathVariable String id) {
        TipoMovimientoResponse response = obtenerTipoMovimientoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<TipoMovimientoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerTiposMovimientoRequest request = ObtenerTiposMovimientoRequest.createFromRequest(params);
        PaginatedResponse<List<TipoMovimientoResponse>> response = obtenerTiposMovimientoUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoMovimientoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarTipoMovimientoRequest request = ActualizarTipoMovimientoRequest.createFromRequest(body);
        TipoMovimientoResponse response = actualizarTipoMovimientoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarTipoMovimientoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
