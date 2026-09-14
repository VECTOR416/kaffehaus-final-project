package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ActualizarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.CrearLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.EliminarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerLotePorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerLotesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/lote")
public class LoteControllerV1 {

    private final CrearLoteUseCase crearLoteUseCase;
    private final ActualizarLoteUseCase actualizarLoteUseCase;
    private final ObtenerLotePorIdUseCase obtenerLotePorIdUseCase;
    private final ObtenerLotesUseCase obtenerLotesUseCase;
    private final EliminarLoteUseCase eliminarLoteUseCase;

    public LoteControllerV1(
            CrearLoteUseCase crearLoteUseCase,
            ActualizarLoteUseCase actualizarLoteUseCase,
            ObtenerLotePorIdUseCase obtenerLotePorIdUseCase,
            ObtenerLotesUseCase obtenerLotesUseCase,
            EliminarLoteUseCase eliminarLoteUseCase) {
        this.crearLoteUseCase = crearLoteUseCase;
        this.actualizarLoteUseCase = actualizarLoteUseCase;
        this.obtenerLotePorIdUseCase = obtenerLotePorIdUseCase;
        this.obtenerLotesUseCase = obtenerLotesUseCase;
        this.eliminarLoteUseCase = eliminarLoteUseCase;
    }

    @PostMapping
    public ResponseEntity<LoteResponse> crear(@RequestBody Map<String, Object> body) {
        CrearLoteRequest request = CrearLoteRequest.createFromRequest(body);
        LoteResponse response = crearLoteUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponse> obtenerPorId(@PathVariable String id) {
        LoteResponse response = obtenerLotePorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<LoteResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerLotesRequest request = ObtenerLotesRequest.createFromRequest(params);
        PaginatedResponse<List<LoteResponse>> response = obtenerLotesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarLoteRequest request = ActualizarLoteRequest.createFromRequest(body);
        LoteResponse response = actualizarLoteUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarLoteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
