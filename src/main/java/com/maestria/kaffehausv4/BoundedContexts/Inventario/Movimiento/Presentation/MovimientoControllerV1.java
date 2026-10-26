package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ActualizarMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.CrearMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.ActualizarMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.CrearMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.EliminarMovimientoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.ObtenerMovimientoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.ObtenerMovimientosUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/movimiento")
public class MovimientoControllerV1 {

    private final CrearMovimientoUseCase crearMovimientoUseCase;
    private final ActualizarMovimientoUseCase actualizarMovimientoUseCase;
    private final ObtenerMovimientoPorIdUseCase obtenerMovimientoPorIdUseCase;
    private final ObtenerMovimientosUseCase obtenerMovimientosUseCase;
    private final EliminarMovimientoUseCase eliminarMovimientoUseCase;

    public MovimientoControllerV1(
            CrearMovimientoUseCase crearMovimientoUseCase,
            ActualizarMovimientoUseCase actualizarMovimientoUseCase,
            ObtenerMovimientoPorIdUseCase obtenerMovimientoPorIdUseCase,
            ObtenerMovimientosUseCase obtenerMovimientosUseCase,
            EliminarMovimientoUseCase eliminarMovimientoUseCase) {
        this.crearMovimientoUseCase = crearMovimientoUseCase;
        this.actualizarMovimientoUseCase = actualizarMovimientoUseCase;
        this.obtenerMovimientoPorIdUseCase = obtenerMovimientoPorIdUseCase;
        this.obtenerMovimientosUseCase = obtenerMovimientosUseCase;
        this.eliminarMovimientoUseCase = eliminarMovimientoUseCase;
    }

    @PostMapping
    public ResponseEntity<MovimientoResponse> crear(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        CrearMovimientoRequest request = CrearMovimientoRequest.createFromRequest(body, idUsuario);
        MovimientoResponse response = crearMovimientoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoResponse> obtenerPorId(@PathVariable String id) {
        MovimientoResponse response = obtenerMovimientoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<MovimientoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerMovimientosRequest request = ObtenerMovimientosRequest.createFromRequest(params);
        PaginatedResponse<List<MovimientoResponse>> response = obtenerMovimientosUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovimientoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarMovimientoRequest request = ActualizarMovimientoRequest.createFromRequest(body);
        MovimientoResponse response = actualizarMovimientoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarMovimientoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
