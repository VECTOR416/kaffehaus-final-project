package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Presentation;
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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ActualizarRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.CrearRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases.ActualizarRolUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases.CrearRolUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases.EliminarRolUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases.ObtenerRolPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases.ObtenerRolesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/rol")
public class RolControllerV1 {

    private final CrearRolUseCase crearRolUseCase;
    private final ActualizarRolUseCase actualizarRolUseCase;
    private final ObtenerRolPorIdUseCase obtenerRolPorIdUseCase;
    private final ObtenerRolesUseCase obtenerRolesUseCase;
    private final EliminarRolUseCase eliminarRolUseCase;

    public RolControllerV1(
            CrearRolUseCase crearRolUseCase,
            ActualizarRolUseCase actualizarRolUseCase,
            ObtenerRolPorIdUseCase obtenerRolPorIdUseCase,
            ObtenerRolesUseCase obtenerRolesUseCase,
            EliminarRolUseCase eliminarRolUseCase) {
        this.crearRolUseCase = crearRolUseCase;
        this.actualizarRolUseCase = actualizarRolUseCase;
        this.obtenerRolPorIdUseCase = obtenerRolPorIdUseCase;
        this.obtenerRolesUseCase = obtenerRolesUseCase;
        this.eliminarRolUseCase = eliminarRolUseCase;
    }

    @RequiereRol("Administrador")
    @PostMapping
    public ResponseEntity<RolResponse> crear(@RequestBody Map<String, Object> body) {
        CrearRolRequest request = CrearRolRequest.createFromRequest(body);
        RolResponse response = crearRolUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> obtenerPorId(@PathVariable String id) {
        RolResponse response = obtenerRolPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<RolResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerRolesRequest request = ObtenerRolesRequest.createFromRequest(params);
        PaginatedResponse<List<RolResponse>> response = obtenerRolesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @RequiereRol("Administrador")
    @PutMapping("/{id}")
    public ResponseEntity<RolResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarRolRequest request = ActualizarRolRequest.createFromRequest(body);
        RolResponse response = actualizarRolUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @RequiereRol("Administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarRolUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
