package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ActualizarEstadoExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.CrearEstadoExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.EstadoExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ObtenerEstadosExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.ActualizarEstadoExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.CrearEstadoExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.EliminarEstadoExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.ObtenerEstadoExistenciaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.ObtenerEstadosExistenciaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/estado-existencia")
public class EstadoExistenciaControllerV1 {

    private final CrearEstadoExistenciaUseCase crearEstadoExistenciaUseCase;
    private final ActualizarEstadoExistenciaUseCase actualizarEstadoExistenciaUseCase;
    private final ObtenerEstadoExistenciaPorIdUseCase obtenerEstadoExistenciaPorIdUseCase;
    private final ObtenerEstadosExistenciaUseCase obtenerEstadosExistenciaUseCase;
    private final EliminarEstadoExistenciaUseCase eliminarEstadoExistenciaUseCase;

    public EstadoExistenciaControllerV1(
            CrearEstadoExistenciaUseCase crearEstadoExistenciaUseCase,
            ActualizarEstadoExistenciaUseCase actualizarEstadoExistenciaUseCase,
            ObtenerEstadoExistenciaPorIdUseCase obtenerEstadoExistenciaPorIdUseCase,
            ObtenerEstadosExistenciaUseCase obtenerEstadosExistenciaUseCase,
            EliminarEstadoExistenciaUseCase eliminarEstadoExistenciaUseCase) {
        this.crearEstadoExistenciaUseCase = crearEstadoExistenciaUseCase;
        this.actualizarEstadoExistenciaUseCase = actualizarEstadoExistenciaUseCase;
        this.obtenerEstadoExistenciaPorIdUseCase = obtenerEstadoExistenciaPorIdUseCase;
        this.obtenerEstadosExistenciaUseCase = obtenerEstadosExistenciaUseCase;
        this.eliminarEstadoExistenciaUseCase = eliminarEstadoExistenciaUseCase;
    }

    @PostMapping
    public ResponseEntity<EstadoExistenciaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearEstadoExistenciaRequest request = CrearEstadoExistenciaRequest.createFromRequest(body);
        EstadoExistenciaResponse response = crearEstadoExistenciaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoExistenciaResponse> obtenerPorId(@PathVariable String id) {
        EstadoExistenciaResponse response = obtenerEstadoExistenciaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<EstadoExistenciaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerEstadosExistenciaRequest request = ObtenerEstadosExistenciaRequest.createFromRequest(params);
        PaginatedResponse<List<EstadoExistenciaResponse>> response = obtenerEstadosExistenciaUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstadoExistenciaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarEstadoExistenciaRequest request = ActualizarEstadoExistenciaRequest.createFromRequest(body);
        EstadoExistenciaResponse response = actualizarEstadoExistenciaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    // Solo un Administrador da de baja recetas y filas de catalogo (logica.md GENERAL-17).
    @RequiereRol("Administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarEstadoExistenciaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
