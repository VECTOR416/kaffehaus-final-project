package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ActualizarDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.CrearDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ObtenerDetallesLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases.ActualizarDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases.CrearDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases.EliminarDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases.ObtenerDetalleLotePorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases.ObtenerDetallesLoteUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/detalle-lote")
public class DetalleLoteControllerV1 {

    private final CrearDetalleLoteUseCase crearDetalleLoteUseCase;
    private final ActualizarDetalleLoteUseCase actualizarDetalleLoteUseCase;
    private final ObtenerDetalleLotePorIdUseCase obtenerDetalleLotePorIdUseCase;
    private final ObtenerDetallesLoteUseCase obtenerDetallesLoteUseCase;
    private final EliminarDetalleLoteUseCase eliminarDetalleLoteUseCase;

    public DetalleLoteControllerV1(
            CrearDetalleLoteUseCase crearDetalleLoteUseCase,
            ActualizarDetalleLoteUseCase actualizarDetalleLoteUseCase,
            ObtenerDetalleLotePorIdUseCase obtenerDetalleLotePorIdUseCase,
            ObtenerDetallesLoteUseCase obtenerDetallesLoteUseCase,
            EliminarDetalleLoteUseCase eliminarDetalleLoteUseCase) {
        this.crearDetalleLoteUseCase = crearDetalleLoteUseCase;
        this.actualizarDetalleLoteUseCase = actualizarDetalleLoteUseCase;
        this.obtenerDetalleLotePorIdUseCase = obtenerDetalleLotePorIdUseCase;
        this.obtenerDetallesLoteUseCase = obtenerDetallesLoteUseCase;
        this.eliminarDetalleLoteUseCase = eliminarDetalleLoteUseCase;
    }

    @PostMapping
    public ResponseEntity<DetalleLoteResponse> crear(@RequestBody Map<String, Object> body) {
        CrearDetalleLoteRequest request = CrearDetalleLoteRequest.createFromRequest(body);
        DetalleLoteResponse response = crearDetalleLoteUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleLoteResponse> obtenerPorId(@PathVariable String id) {
        DetalleLoteResponse response = obtenerDetalleLotePorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<DetalleLoteResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerDetallesLoteRequest request = ObtenerDetallesLoteRequest.createFromRequest(params);
        PaginatedResponse<List<DetalleLoteResponse>> response = obtenerDetallesLoteUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleLoteResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarDetalleLoteRequest request = ActualizarDetalleLoteRequest.createFromRequest(body);
        DetalleLoteResponse response = actualizarDetalleLoteUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarDetalleLoteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
