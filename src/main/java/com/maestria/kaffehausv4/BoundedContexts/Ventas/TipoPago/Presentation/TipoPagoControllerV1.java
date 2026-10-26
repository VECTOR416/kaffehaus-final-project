package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ActualizarTipoPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.CrearTipoPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.TipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.ActualizarTipoPagoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.CrearTipoPagoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.EliminarTipoPagoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.ObtenerTipoPagoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.ObtenerTiposPagoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/tipo-pago")
public class TipoPagoControllerV1 {

    private final CrearTipoPagoUseCase crearTipoPagoUseCase;
    private final ActualizarTipoPagoUseCase actualizarTipoPagoUseCase;
    private final ObtenerTipoPagoPorIdUseCase obtenerTipoPagoPorIdUseCase;
    private final ObtenerTiposPagoUseCase obtenerTiposPagoUseCase;
    private final EliminarTipoPagoUseCase eliminarTipoPagoUseCase;

    public TipoPagoControllerV1(
            CrearTipoPagoUseCase crearTipoPagoUseCase,
            ActualizarTipoPagoUseCase actualizarTipoPagoUseCase,
            ObtenerTipoPagoPorIdUseCase obtenerTipoPagoPorIdUseCase,
            ObtenerTiposPagoUseCase obtenerTiposPagoUseCase,
            EliminarTipoPagoUseCase eliminarTipoPagoUseCase) {
        this.crearTipoPagoUseCase = crearTipoPagoUseCase;
        this.actualizarTipoPagoUseCase = actualizarTipoPagoUseCase;
        this.obtenerTipoPagoPorIdUseCase = obtenerTipoPagoPorIdUseCase;
        this.obtenerTiposPagoUseCase = obtenerTiposPagoUseCase;
        this.eliminarTipoPagoUseCase = eliminarTipoPagoUseCase;
    }

    @PostMapping
    public ResponseEntity<TipoPagoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearTipoPagoRequest request = CrearTipoPagoRequest.createFromRequest(body);
        TipoPagoResponse response = crearTipoPagoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoPagoResponse> obtenerPorId(@PathVariable String id) {
        TipoPagoResponse response = obtenerTipoPagoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<TipoPagoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerTiposPagoRequest request = ObtenerTiposPagoRequest.createFromRequest(params);
        PaginatedResponse<List<TipoPagoResponse>> response = obtenerTiposPagoUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoPagoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarTipoPagoRequest request = ActualizarTipoPagoRequest.createFromRequest(body);
        TipoPagoResponse response = actualizarTipoPagoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    // Solo un Administrador da de baja recetas y filas de catalogo (logica.md GENERAL-17).
    @RequiereRol("Administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarTipoPagoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
