package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ActualizarTipoDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.CrearTipoDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases.ActualizarTipoDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases.CrearTipoDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases.EliminarTipoDescuentoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases.ObtenerTipoDescuentoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases.ObtenerTiposDescuentoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/tipo-descuento")
public class TipoDescuentoControllerV1 {

    private final CrearTipoDescuentoUseCase crearTipoDescuentoUseCase;
    private final ActualizarTipoDescuentoUseCase actualizarTipoDescuentoUseCase;
    private final ObtenerTipoDescuentoPorIdUseCase obtenerTipoDescuentoPorIdUseCase;
    private final ObtenerTiposDescuentoUseCase obtenerTiposDescuentoUseCase;
    private final EliminarTipoDescuentoUseCase eliminarTipoDescuentoUseCase;

    public TipoDescuentoControllerV1(
            CrearTipoDescuentoUseCase crearTipoDescuentoUseCase,
            ActualizarTipoDescuentoUseCase actualizarTipoDescuentoUseCase,
            ObtenerTipoDescuentoPorIdUseCase obtenerTipoDescuentoPorIdUseCase,
            ObtenerTiposDescuentoUseCase obtenerTiposDescuentoUseCase,
            EliminarTipoDescuentoUseCase eliminarTipoDescuentoUseCase) {
        this.crearTipoDescuentoUseCase = crearTipoDescuentoUseCase;
        this.actualizarTipoDescuentoUseCase = actualizarTipoDescuentoUseCase;
        this.obtenerTipoDescuentoPorIdUseCase = obtenerTipoDescuentoPorIdUseCase;
        this.obtenerTiposDescuentoUseCase = obtenerTiposDescuentoUseCase;
        this.eliminarTipoDescuentoUseCase = eliminarTipoDescuentoUseCase;
    }

    @PostMapping
    public ResponseEntity<TipoDescuentoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearTipoDescuentoRequest request = CrearTipoDescuentoRequest.createFromRequest(body);
        TipoDescuentoResponse response = crearTipoDescuentoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoDescuentoResponse> obtenerPorId(@PathVariable String id) {
        TipoDescuentoResponse response = obtenerTipoDescuentoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<TipoDescuentoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerTiposDescuentoRequest request = ObtenerTiposDescuentoRequest.createFromRequest(params);
        PaginatedResponse<List<TipoDescuentoResponse>> response = obtenerTiposDescuentoUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoDescuentoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarTipoDescuentoRequest request = ActualizarTipoDescuentoRequest.createFromRequest(body);
        TipoDescuentoResponse response = actualizarTipoDescuentoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarTipoDescuentoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
