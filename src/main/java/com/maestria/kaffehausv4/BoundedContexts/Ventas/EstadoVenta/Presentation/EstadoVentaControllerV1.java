package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ActualizarEstadoVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.CrearEstadoVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases.ActualizarEstadoVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases.CrearEstadoVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases.EliminarEstadoVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases.ObtenerEstadoVentaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases.ObtenerEstadosVentaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/estado-venta")
public class EstadoVentaControllerV1 {

    private final CrearEstadoVentaUseCase crearEstadoVentaUseCase;
    private final ActualizarEstadoVentaUseCase actualizarEstadoVentaUseCase;
    private final ObtenerEstadoVentaPorIdUseCase obtenerEstadoVentaPorIdUseCase;
    private final ObtenerEstadosVentaUseCase obtenerEstadosVentaUseCase;
    private final EliminarEstadoVentaUseCase eliminarEstadoVentaUseCase;

    public EstadoVentaControllerV1(
            CrearEstadoVentaUseCase crearEstadoVentaUseCase,
            ActualizarEstadoVentaUseCase actualizarEstadoVentaUseCase,
            ObtenerEstadoVentaPorIdUseCase obtenerEstadoVentaPorIdUseCase,
            ObtenerEstadosVentaUseCase obtenerEstadosVentaUseCase,
            EliminarEstadoVentaUseCase eliminarEstadoVentaUseCase) {
        this.crearEstadoVentaUseCase = crearEstadoVentaUseCase;
        this.actualizarEstadoVentaUseCase = actualizarEstadoVentaUseCase;
        this.obtenerEstadoVentaPorIdUseCase = obtenerEstadoVentaPorIdUseCase;
        this.obtenerEstadosVentaUseCase = obtenerEstadosVentaUseCase;
        this.eliminarEstadoVentaUseCase = eliminarEstadoVentaUseCase;
    }

    @PostMapping
    public ResponseEntity<EstadoVentaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearEstadoVentaRequest request = CrearEstadoVentaRequest.createFromRequest(body);
        EstadoVentaResponse response = crearEstadoVentaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoVentaResponse> obtenerPorId(@PathVariable String id) {
        EstadoVentaResponse response = obtenerEstadoVentaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<EstadoVentaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerEstadosVentaRequest request = ObtenerEstadosVentaRequest.createFromRequest(params);
        PaginatedResponse<List<EstadoVentaResponse>> response = obtenerEstadosVentaUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstadoVentaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarEstadoVentaRequest request = ActualizarEstadoVentaRequest.createFromRequest(body);
        EstadoVentaResponse response = actualizarEstadoVentaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarEstadoVentaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
