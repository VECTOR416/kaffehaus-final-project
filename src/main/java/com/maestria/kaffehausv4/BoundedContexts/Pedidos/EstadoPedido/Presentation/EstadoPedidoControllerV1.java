package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ActualizarEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.CrearEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases.ActualizarEstadoPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases.CrearEstadoPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases.EliminarEstadoPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases.ObtenerEstadoPedidoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases.ObtenerEstadosPedidoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/estado-pedido")
public class EstadoPedidoControllerV1 {

    private final CrearEstadoPedidoUseCase crearEstadoPedidoUseCase;
    private final ActualizarEstadoPedidoUseCase actualizarEstadoPedidoUseCase;
    private final ObtenerEstadoPedidoPorIdUseCase obtenerEstadoPedidoPorIdUseCase;
    private final ObtenerEstadosPedidoUseCase obtenerEstadosPedidoUseCase;
    private final EliminarEstadoPedidoUseCase eliminarEstadoPedidoUseCase;

    public EstadoPedidoControllerV1(
            CrearEstadoPedidoUseCase crearEstadoPedidoUseCase,
            ActualizarEstadoPedidoUseCase actualizarEstadoPedidoUseCase,
            ObtenerEstadoPedidoPorIdUseCase obtenerEstadoPedidoPorIdUseCase,
            ObtenerEstadosPedidoUseCase obtenerEstadosPedidoUseCase,
            EliminarEstadoPedidoUseCase eliminarEstadoPedidoUseCase) {
        this.crearEstadoPedidoUseCase = crearEstadoPedidoUseCase;
        this.actualizarEstadoPedidoUseCase = actualizarEstadoPedidoUseCase;
        this.obtenerEstadoPedidoPorIdUseCase = obtenerEstadoPedidoPorIdUseCase;
        this.obtenerEstadosPedidoUseCase = obtenerEstadosPedidoUseCase;
        this.eliminarEstadoPedidoUseCase = eliminarEstadoPedidoUseCase;
    }

    @PostMapping
    public ResponseEntity<EstadoPedidoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearEstadoPedidoRequest request = CrearEstadoPedidoRequest.createFromRequest(body);
        EstadoPedidoResponse response = crearEstadoPedidoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoPedidoResponse> obtenerPorId(@PathVariable String id) {
        EstadoPedidoResponse response = obtenerEstadoPedidoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<EstadoPedidoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerEstadosPedidoRequest request = ObtenerEstadosPedidoRequest.createFromRequest(params);
        PaginatedResponse<List<EstadoPedidoResponse>> response = obtenerEstadosPedidoUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstadoPedidoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarEstadoPedidoRequest request = ActualizarEstadoPedidoRequest.createFromRequest(body);
        EstadoPedidoResponse response = actualizarEstadoPedidoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarEstadoPedidoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
