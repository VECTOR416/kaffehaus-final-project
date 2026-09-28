package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ActualizarClienteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ClienteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.CrearClienteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases.ActualizarClienteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases.CrearClienteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases.EliminarClienteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases.ObtenerClientePorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases.ObtenerClientesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/cliente")
public class ClienteControllerV1 {

    private final CrearClienteUseCase crearClienteUseCase;
    private final ActualizarClienteUseCase actualizarClienteUseCase;
    private final ObtenerClientePorIdUseCase obtenerClientePorIdUseCase;
    private final ObtenerClientesUseCase obtenerClientesUseCase;
    private final EliminarClienteUseCase eliminarClienteUseCase;

    public ClienteControllerV1(
            CrearClienteUseCase crearClienteUseCase,
            ActualizarClienteUseCase actualizarClienteUseCase,
            ObtenerClientePorIdUseCase obtenerClientePorIdUseCase,
            ObtenerClientesUseCase obtenerClientesUseCase,
            EliminarClienteUseCase eliminarClienteUseCase) {
        this.crearClienteUseCase = crearClienteUseCase;
        this.actualizarClienteUseCase = actualizarClienteUseCase;
        this.obtenerClientePorIdUseCase = obtenerClientePorIdUseCase;
        this.obtenerClientesUseCase = obtenerClientesUseCase;
        this.eliminarClienteUseCase = eliminarClienteUseCase;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@RequestBody Map<String, Object> body) {
        CrearClienteRequest request = CrearClienteRequest.createFromRequest(body);
        ClienteResponse response = crearClienteUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable String id) {
        ClienteResponse response = obtenerClientePorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ClienteResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerClientesRequest request = ObtenerClientesRequest.createFromRequest(params);
        PaginatedResponse<List<ClienteResponse>> response = obtenerClientesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarClienteRequest request = ActualizarClienteRequest.createFromRequest(body);
        ClienteResponse response = actualizarClienteUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarClienteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
