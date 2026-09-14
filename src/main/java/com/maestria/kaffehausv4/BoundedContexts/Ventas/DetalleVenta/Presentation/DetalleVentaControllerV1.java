package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ActualizarDetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.CrearDetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.DetalleVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ObtenerDetallesVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases.ActualizarDetalleVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases.CrearDetalleVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases.EliminarDetalleVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases.ObtenerDetalleVentaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases.ObtenerDetallesVentaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/detalle-venta")
public class DetalleVentaControllerV1 {

    private final CrearDetalleVentaUseCase crearDetalleVentaUseCase;
    private final ActualizarDetalleVentaUseCase actualizarDetalleVentaUseCase;
    private final ObtenerDetalleVentaPorIdUseCase obtenerDetalleVentaPorIdUseCase;
    private final ObtenerDetallesVentaUseCase obtenerDetallesVentaUseCase;
    private final EliminarDetalleVentaUseCase eliminarDetalleVentaUseCase;

    public DetalleVentaControllerV1(
            CrearDetalleVentaUseCase crearDetalleVentaUseCase,
            ActualizarDetalleVentaUseCase actualizarDetalleVentaUseCase,
            ObtenerDetalleVentaPorIdUseCase obtenerDetalleVentaPorIdUseCase,
            ObtenerDetallesVentaUseCase obtenerDetallesVentaUseCase,
            EliminarDetalleVentaUseCase eliminarDetalleVentaUseCase) {
        this.crearDetalleVentaUseCase = crearDetalleVentaUseCase;
        this.actualizarDetalleVentaUseCase = actualizarDetalleVentaUseCase;
        this.obtenerDetalleVentaPorIdUseCase = obtenerDetalleVentaPorIdUseCase;
        this.obtenerDetallesVentaUseCase = obtenerDetallesVentaUseCase;
        this.eliminarDetalleVentaUseCase = eliminarDetalleVentaUseCase;
    }

    @PostMapping
    public ResponseEntity<DetalleVentaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearDetalleVentaRequest request = CrearDetalleVentaRequest.createFromRequest(body);
        DetalleVentaResponse response = crearDetalleVentaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleVentaResponse> obtenerPorId(@PathVariable String id) {
        DetalleVentaResponse response = obtenerDetalleVentaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<DetalleVentaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerDetallesVentaRequest request = ObtenerDetallesVentaRequest.createFromRequest(params);
        PaginatedResponse<List<DetalleVentaResponse>> response = obtenerDetallesVentaUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleVentaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarDetalleVentaRequest request = ActualizarDetalleVentaRequest.createFromRequest(body);
        DetalleVentaResponse response = actualizarDetalleVentaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarDetalleVentaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
