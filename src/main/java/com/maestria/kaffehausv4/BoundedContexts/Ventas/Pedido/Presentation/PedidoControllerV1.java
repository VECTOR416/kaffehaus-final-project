package com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases.ActualizarPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases.CrearPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases.EliminarPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases.ObtenerPedidoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases.ObtenerPedidosUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/pedido")
public class PedidoControllerV1 {

    private final CrearPedidoUseCase crearPedidoUseCase;
    private final ActualizarPedidoUseCase actualizarPedidoUseCase;
    private final ObtenerPedidoPorIdUseCase obtenerPedidoPorIdUseCase;
    private final ObtenerPedidosUseCase obtenerPedidosUseCase;
    private final EliminarPedidoUseCase eliminarPedidoUseCase;

    public PedidoControllerV1(
            CrearPedidoUseCase crearPedidoUseCase,
            ActualizarPedidoUseCase actualizarPedidoUseCase,
            ObtenerPedidoPorIdUseCase obtenerPedidoPorIdUseCase,
            ObtenerPedidosUseCase obtenerPedidosUseCase,
            EliminarPedidoUseCase eliminarPedidoUseCase) {
        this.crearPedidoUseCase = crearPedidoUseCase;
        this.actualizarPedidoUseCase = actualizarPedidoUseCase;
        this.obtenerPedidoPorIdUseCase = obtenerPedidoPorIdUseCase;
        this.obtenerPedidosUseCase = obtenerPedidosUseCase;
        this.eliminarPedidoUseCase = eliminarPedidoUseCase;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearPedidoRequest request = CrearPedidoRequest.createFromRequest(body);
        PedidoResponse response = crearPedidoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPorId(@PathVariable String id) {
        PedidoResponse response = obtenerPedidoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<PedidoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerPedidosRequest request = ObtenerPedidosRequest.createFromRequest(params);
        PaginatedResponse<List<PedidoResponse>> response = obtenerPedidosUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarPedidoRequest request = ActualizarPedidoRequest.createFromRequest(body);
        PedidoResponse response = actualizarPedidoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarPedidoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
