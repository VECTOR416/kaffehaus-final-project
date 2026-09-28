package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ActualizarProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.CrearProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.ActualizarProveedorUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.CrearProveedorUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.EliminarProveedorUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.ObtenerProveedorPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases.ObtenerProveedoresUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/proveedor")
public class ProveedorControllerV1 {

    private final CrearProveedorUseCase crearProveedorUseCase;
    private final ActualizarProveedorUseCase actualizarProveedorUseCase;
    private final ObtenerProveedorPorIdUseCase obtenerProveedorPorIdUseCase;
    private final ObtenerProveedoresUseCase obtenerProveedoresUseCase;
    private final EliminarProveedorUseCase eliminarProveedorUseCase;

    public ProveedorControllerV1(
            CrearProveedorUseCase crearProveedorUseCase,
            ActualizarProveedorUseCase actualizarProveedorUseCase,
            ObtenerProveedorPorIdUseCase obtenerProveedorPorIdUseCase,
            ObtenerProveedoresUseCase obtenerProveedoresUseCase,
            EliminarProveedorUseCase eliminarProveedorUseCase) {
        this.crearProveedorUseCase = crearProveedorUseCase;
        this.actualizarProveedorUseCase = actualizarProveedorUseCase;
        this.obtenerProveedorPorIdUseCase = obtenerProveedorPorIdUseCase;
        this.obtenerProveedoresUseCase = obtenerProveedoresUseCase;
        this.eliminarProveedorUseCase = eliminarProveedorUseCase;
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@RequestBody Map<String, Object> body) {
        CrearProveedorRequest request = CrearProveedorRequest.createFromRequest(body);
        ProveedorResponse response = crearProveedorUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtenerPorId(@PathVariable String id) {
        ProveedorResponse response = obtenerProveedorPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ProveedorResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerProveedoresRequest request = ObtenerProveedoresRequest.createFromRequest(params);
        PaginatedResponse<List<ProveedorResponse>> response = obtenerProveedoresUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarProveedorRequest request = ActualizarProveedorRequest.createFromRequest(body);
        ProveedorResponse response = actualizarProveedorUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarProveedorUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
