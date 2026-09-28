package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ActualizarVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.AnularVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.EliminarVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentasUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/venta")
public class VentaControllerV1 {

    private final CrearVentaUseCase crearVentaUseCase;
    private final ActualizarVentaUseCase actualizarVentaUseCase;
    private final ObtenerVentaPorIdUseCase obtenerVentaPorIdUseCase;
    private final ObtenerVentasUseCase obtenerVentasUseCase;
    private final EliminarVentaUseCase eliminarVentaUseCase;
    private final AnularVentaUseCase anularVentaUseCase;

    public VentaControllerV1(
            CrearVentaUseCase crearVentaUseCase,
            ActualizarVentaUseCase actualizarVentaUseCase,
            ObtenerVentaPorIdUseCase obtenerVentaPorIdUseCase,
            ObtenerVentasUseCase obtenerVentasUseCase,
            EliminarVentaUseCase eliminarVentaUseCase,
            AnularVentaUseCase anularVentaUseCase) {
        this.crearVentaUseCase = crearVentaUseCase;
        this.actualizarVentaUseCase = actualizarVentaUseCase;
        this.obtenerVentaPorIdUseCase = obtenerVentaPorIdUseCase;
        this.obtenerVentasUseCase = obtenerVentasUseCase;
        this.eliminarVentaUseCase = eliminarVentaUseCase;
        this.anularVentaUseCase = anularVentaUseCase;
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crear(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        CrearVentaRequest request = CrearVentaRequest.createFromRequest(body, idUsuario);
        VentaResponse response = crearVentaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtenerPorId(@PathVariable String id) {
        VentaResponse response = obtenerVentaPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<VentaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerVentasRequest request = ObtenerVentasRequest.createFromRequest(params);
        PaginatedResponse<List<VentaResponse>> response = obtenerVentasUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VentaResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarVentaRequest request = ActualizarVentaRequest.createFromRequest(body);
        VentaResponse response = actualizarVentaUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarVentaUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // Unico modo de deshacer una venta (ventas.md VEN-40).
    @PostMapping("/{id}/anulacion")
    public ResponseEntity<VentaResponse> anular(@PathVariable String id, @RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        AnularVentaRequest request = AnularVentaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.ok(anularVentaUseCase.execute(id, request));
    }
}
