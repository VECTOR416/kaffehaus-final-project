package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ActualizarUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.CrearUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.UsuarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.ActualizarUsuarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.CrearUsuarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.EliminarUsuarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.ObtenerUsuarioPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.ObtenerUsuariosUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/usuario")
public class UsuarioControllerV1 {

    private final CrearUsuarioUseCase crearUsuarioUseCase;
    private final ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    private final ObtenerUsuarioPorIdUseCase obtenerUsuarioPorIdUseCase;
    private final ObtenerUsuariosUseCase obtenerUsuariosUseCase;
    private final EliminarUsuarioUseCase eliminarUsuarioUseCase;

    public UsuarioControllerV1(
            CrearUsuarioUseCase crearUsuarioUseCase,
            ActualizarUsuarioUseCase actualizarUsuarioUseCase,
            ObtenerUsuarioPorIdUseCase obtenerUsuarioPorIdUseCase,
            ObtenerUsuariosUseCase obtenerUsuariosUseCase,
            EliminarUsuarioUseCase eliminarUsuarioUseCase) {
        this.crearUsuarioUseCase = crearUsuarioUseCase;
        this.actualizarUsuarioUseCase = actualizarUsuarioUseCase;
        this.obtenerUsuarioPorIdUseCase = obtenerUsuarioPorIdUseCase;
        this.obtenerUsuariosUseCase = obtenerUsuariosUseCase;
        this.eliminarUsuarioUseCase = eliminarUsuarioUseCase;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody Map<String, Object> body) {
        CrearUsuarioRequest request = CrearUsuarioRequest.createFromRequest(body);
        UsuarioResponse response = crearUsuarioUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable String id) {
        UsuarioResponse response = obtenerUsuarioPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<UsuarioResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerUsuariosRequest request = ObtenerUsuariosRequest.createFromRequest(params);
        PaginatedResponse<List<UsuarioResponse>> response = obtenerUsuariosUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarUsuarioRequest request = ActualizarUsuarioRequest.createFromRequest(body);
        UsuarioResponse response = actualizarUsuarioUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarUsuarioUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
