package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ActualizarEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.CrearEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases.ActualizarEntidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases.CrearEntidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases.EliminarEntidadUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases.ObtenerEntidadPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases.ObtenerEntidadesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/entidad")
public class EntidadControllerV1 {

    private final CrearEntidadUseCase crearEntidadUseCase;
    private final ActualizarEntidadUseCase actualizarEntidadUseCase;
    private final ObtenerEntidadPorIdUseCase obtenerEntidadPorIdUseCase;
    private final ObtenerEntidadesUseCase obtenerEntidadesUseCase;
    private final EliminarEntidadUseCase eliminarEntidadUseCase;

    public EntidadControllerV1(
            CrearEntidadUseCase crearEntidadUseCase,
            ActualizarEntidadUseCase actualizarEntidadUseCase,
            ObtenerEntidadPorIdUseCase obtenerEntidadPorIdUseCase,
            ObtenerEntidadesUseCase obtenerEntidadesUseCase,
            EliminarEntidadUseCase eliminarEntidadUseCase) {
        this.crearEntidadUseCase = crearEntidadUseCase;
        this.actualizarEntidadUseCase = actualizarEntidadUseCase;
        this.obtenerEntidadPorIdUseCase = obtenerEntidadPorIdUseCase;
        this.obtenerEntidadesUseCase = obtenerEntidadesUseCase;
        this.eliminarEntidadUseCase = eliminarEntidadUseCase;
    }

    @PostMapping
    public ResponseEntity<EntidadResponse> crear(@RequestBody Map<String, Object> body) {
        CrearEntidadRequest request = CrearEntidadRequest.createFromRequest(body);
        EntidadResponse response = crearEntidadUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadResponse> obtenerPorId(@PathVariable String id) {
        EntidadResponse response = obtenerEntidadPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<EntidadResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerEntidadesRequest request = ObtenerEntidadesRequest.createFromRequest(params);
        PaginatedResponse<List<EntidadResponse>> response = obtenerEntidadesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntidadResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarEntidadRequest request = ActualizarEntidadRequest.createFromRequest(body);
        EntidadResponse response = actualizarEntidadUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarEntidadUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
