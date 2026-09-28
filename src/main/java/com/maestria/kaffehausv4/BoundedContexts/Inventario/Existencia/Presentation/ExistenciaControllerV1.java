package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ActualizarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.CrearExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.ActualizarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.CrearExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.EliminarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.ObtenerExistenciaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.ObtenerExistenciasUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.AjustarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarMermaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarVencimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.AjustarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarMermaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarVencimientoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/existencia")
public class ExistenciaControllerV1 {

    private final CrearExistenciaUseCase crearExistenciaUseCase;
    private final ActualizarExistenciaUseCase actualizarExistenciaUseCase;
    private final ObtenerExistenciaPorIdUseCase obtenerExistenciaPorIdUseCase;
    private final ObtenerExistenciasUseCase obtenerExistenciasUseCase;
    private final EliminarExistenciaUseCase eliminarExistenciaUseCase;
    private final RegistrarMermaUseCase registrarMermaUseCase;
    private final RegistrarVencimientoUseCase registrarVencimientoUseCase;
    private final AjustarExistenciaUseCase ajustarExistenciaUseCase;

    public ExistenciaControllerV1(
            CrearExistenciaUseCase crearExistenciaUseCase,
            ActualizarExistenciaUseCase actualizarExistenciaUseCase,
            ObtenerExistenciaPorIdUseCase obtenerExistenciaPorIdUseCase,
            ObtenerExistenciasUseCase obtenerExistenciasUseCase,
            EliminarExistenciaUseCase eliminarExistenciaUseCase,
            RegistrarMermaUseCase registrarMermaUseCase,
            RegistrarVencimientoUseCase registrarVencimientoUseCase,
            AjustarExistenciaUseCase ajustarExistenciaUseCase) {
        this.crearExistenciaUseCase = crearExistenciaUseCase;
        this.actualizarExistenciaUseCase = actualizarExistenciaUseCase;
        this.obtenerExistenciaPorIdUseCase = obtenerExistenciaPorIdUseCase;
        this.obtenerExistenciasUseCase = obtenerExistenciasUseCase;
        this.eliminarExistenciaUseCase = eliminarExistenciaUseCase;
        this.registrarMermaUseCase = registrarMermaUseCase;
        this.registrarVencimientoUseCase = registrarVencimientoUseCase;
        this.ajustarExistenciaUseCase = ajustarExistenciaUseCase;
    }

    @PostMapping
    public ResponseEntity<ExistenciaResponse> crear(@RequestBody Map<String, Object> body) {
        CrearExistenciaRequest request = CrearExistenciaRequest.createFromRequest(body);
        ExistenciaResponse response = crearExistenciaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExistenciaResponse> obtenerPorId(@PathVariable String id) {
        ExistenciaResponse response = obtenerExistenciaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ExistenciaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerExistenciasRequest request = ObtenerExistenciasRequest.createFromRequest(params);
        PaginatedResponse<List<ExistenciaResponse>> response = obtenerExistenciasUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExistenciaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarExistenciaRequest request = ActualizarExistenciaRequest.createFromRequest(body);
        ExistenciaResponse response = actualizarExistenciaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarExistenciaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // Movimientos operativos sobre una existencia (inventario.md INV-42, INV-43).

    @PostMapping("/{id}/merma")
    public ResponseEntity<ExistenciaResponse> registrarMerma(@PathVariable String id, @RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        RegistrarMermaRequest request = RegistrarMermaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.ok(registrarMermaUseCase.execute(id, request));
    }

    @PostMapping("/{id}/vencimiento")
    public ResponseEntity<ExistenciaResponse> registrarVencimiento(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        RegistrarVencimientoRequest request = RegistrarVencimientoRequest.createFromRequest(body != null ? body : Map.of(), idUsuario);
        return ResponseEntity.ok(registrarVencimientoUseCase.execute(id, request));
    }

    @RequiereRol("Administrador")
    @PostMapping("/{id}/ajuste")
    public ResponseEntity<ExistenciaResponse> ajustar(@PathVariable String id, @RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        AjustarExistenciaRequest request = AjustarExistenciaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.ok(ajustarExistenciaUseCase.execute(id, request));
    }
}
