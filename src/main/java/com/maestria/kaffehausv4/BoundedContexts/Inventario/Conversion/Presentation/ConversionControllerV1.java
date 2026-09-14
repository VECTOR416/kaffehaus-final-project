package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ActualizarConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ConversionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.CrearConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases.ActualizarConversionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases.CrearConversionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases.EliminarConversionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases.ObtenerConversionPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases.ObtenerConversionesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@RestController
@RequestMapping("api/v1/conversion")
public class ConversionControllerV1 {

    private final CrearConversionUseCase crearConversionUseCase;
    private final ActualizarConversionUseCase actualizarConversionUseCase;
    private final ObtenerConversionPorIdUseCase obtenerConversionPorIdUseCase;
    private final ObtenerConversionesUseCase obtenerConversionesUseCase;
    private final EliminarConversionUseCase eliminarConversionUseCase;

    public ConversionControllerV1(
            CrearConversionUseCase crearConversionUseCase,
            ActualizarConversionUseCase actualizarConversionUseCase,
            ObtenerConversionPorIdUseCase obtenerConversionPorIdUseCase,
            ObtenerConversionesUseCase obtenerConversionesUseCase,
            EliminarConversionUseCase eliminarConversionUseCase) {
        this.crearConversionUseCase = crearConversionUseCase;
        this.actualizarConversionUseCase = actualizarConversionUseCase;
        this.obtenerConversionPorIdUseCase = obtenerConversionPorIdUseCase;
        this.obtenerConversionesUseCase = obtenerConversionesUseCase;
        this.eliminarConversionUseCase = eliminarConversionUseCase;
    }

    @PostMapping
    public ResponseEntity<ConversionResponse> crear(@RequestBody Map<String, Object> body) {
        CrearConversionRequest request = CrearConversionRequest.createFromRequest(body);
        ConversionResponse response = crearConversionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversionResponse> obtenerPorId(@PathVariable String id) {
        ConversionResponse response = obtenerConversionPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ConversionResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerConversionesRequest request = ObtenerConversionesRequest.createFromRequest(params);
        PaginatedResponse<List<ConversionResponse>> response = obtenerConversionesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversionResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarConversionRequest request = ActualizarConversionRequest.createFromRequest(body);
        ConversionResponse response = actualizarConversionUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarConversionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
