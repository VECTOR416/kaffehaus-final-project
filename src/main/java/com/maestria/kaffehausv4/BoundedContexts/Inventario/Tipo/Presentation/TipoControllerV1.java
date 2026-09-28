package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ActualizarTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.CrearTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.ActualizarTipoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.CrearTipoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.EliminarTipoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.ObtenerTipoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.ObtenerTiposUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/tipo")
public class TipoControllerV1 {

    private final CrearTipoUseCase crearTipoUseCase;
    private final ActualizarTipoUseCase actualizarTipoUseCase;
    private final ObtenerTipoPorIdUseCase obtenerTipoPorIdUseCase;
    private final ObtenerTiposUseCase obtenerTiposUseCase;
    private final EliminarTipoUseCase eliminarTipoUseCase;

    public TipoControllerV1(
            CrearTipoUseCase crearTipoUseCase,
            ActualizarTipoUseCase actualizarTipoUseCase,
            ObtenerTipoPorIdUseCase obtenerTipoPorIdUseCase,
            ObtenerTiposUseCase obtenerTiposUseCase,
            EliminarTipoUseCase eliminarTipoUseCase) {
        this.crearTipoUseCase = crearTipoUseCase;
        this.actualizarTipoUseCase = actualizarTipoUseCase;
        this.obtenerTipoPorIdUseCase = obtenerTipoPorIdUseCase;
        this.obtenerTiposUseCase = obtenerTiposUseCase;
        this.eliminarTipoUseCase = eliminarTipoUseCase;
    }

    @PostMapping
    public ResponseEntity<TipoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearTipoRequest request = CrearTipoRequest.createFromRequest(body);
        TipoResponse response = crearTipoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoResponse> obtenerPorId(@PathVariable String id) {
        TipoResponse response = obtenerTipoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<TipoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerTiposRequest request = ObtenerTiposRequest.createFromRequest(params);
        PaginatedResponse<List<TipoResponse>> response = obtenerTiposUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarTipoRequest request = ActualizarTipoRequest.createFromRequest(body);
        TipoResponse response = actualizarTipoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarTipoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
