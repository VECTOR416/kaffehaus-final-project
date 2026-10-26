package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ActualizarProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.ActualizarProduccionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.CrearProduccionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.EliminarProduccionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.ObtenerProduccionPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.ObtenerProduccionesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/produccion")
public class ProduccionControllerV1 {

    private final CrearProduccionUseCase crearProduccionUseCase;
    private final ActualizarProduccionUseCase actualizarProduccionUseCase;
    private final ObtenerProduccionPorIdUseCase obtenerProduccionPorIdUseCase;
    private final ObtenerProduccionesUseCase obtenerProduccionesUseCase;
    private final EliminarProduccionUseCase eliminarProduccionUseCase;

    public ProduccionControllerV1(
            CrearProduccionUseCase crearProduccionUseCase,
            ActualizarProduccionUseCase actualizarProduccionUseCase,
            ObtenerProduccionPorIdUseCase obtenerProduccionPorIdUseCase,
            ObtenerProduccionesUseCase obtenerProduccionesUseCase,
            EliminarProduccionUseCase eliminarProduccionUseCase) {
        this.crearProduccionUseCase = crearProduccionUseCase;
        this.actualizarProduccionUseCase = actualizarProduccionUseCase;
        this.obtenerProduccionPorIdUseCase = obtenerProduccionPorIdUseCase;
        this.obtenerProduccionesUseCase = obtenerProduccionesUseCase;
        this.eliminarProduccionUseCase = eliminarProduccionUseCase;
    }

    @PostMapping
    public ResponseEntity<ProduccionResponse> crear(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        CrearProduccionRequest request = CrearProduccionRequest.createFromRequest(body, idUsuario);
        ProduccionResponse response = crearProduccionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduccionResponse> obtenerPorId(@PathVariable String id) {
        ProduccionResponse response = obtenerProduccionPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ProduccionResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerProduccionesRequest request = ObtenerProduccionesRequest.createFromRequest(params);
        PaginatedResponse<List<ProduccionResponse>> response = obtenerProduccionesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProduccionResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarProduccionRequest request = ActualizarProduccionRequest.createFromRequest(body);
        ProduccionResponse response = actualizarProduccionUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarProduccionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
