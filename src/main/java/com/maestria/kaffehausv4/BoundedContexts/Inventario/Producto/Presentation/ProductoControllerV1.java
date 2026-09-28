package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ActualizarProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.CrearProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ActualizarProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.CrearProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.EliminarProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerProductoPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerProductosUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerInventarioProductoUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/producto")
public class ProductoControllerV1 {

    private final CrearProductoUseCase crearProductoUseCase;
    private final ActualizarProductoUseCase actualizarProductoUseCase;
    private final ObtenerProductoPorIdUseCase obtenerProductoPorIdUseCase;
    private final ObtenerProductosUseCase obtenerProductosUseCase;
    private final EliminarProductoUseCase eliminarProductoUseCase;
    private final ObtenerInventarioProductoUseCase obtenerInventarioProductoUseCase;

    public ProductoControllerV1(
            CrearProductoUseCase crearProductoUseCase,
            ActualizarProductoUseCase actualizarProductoUseCase,
            ObtenerProductoPorIdUseCase obtenerProductoPorIdUseCase,
            ObtenerProductosUseCase obtenerProductosUseCase,
            EliminarProductoUseCase eliminarProductoUseCase,
            ObtenerInventarioProductoUseCase obtenerInventarioProductoUseCase) {
        this.crearProductoUseCase = crearProductoUseCase;
        this.actualizarProductoUseCase = actualizarProductoUseCase;
        this.obtenerProductoPorIdUseCase = obtenerProductoPorIdUseCase;
        this.obtenerProductosUseCase = obtenerProductosUseCase;
        this.eliminarProductoUseCase = eliminarProductoUseCase;
        this.obtenerInventarioProductoUseCase = obtenerInventarioProductoUseCase;
    }

    @RequiereRol("Administrador")
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@RequestBody Map<String, Object> body) {
        CrearProductoRequest request = CrearProductoRequest.createFromRequest(body);
        ProductoResponse response = crearProductoUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable String id) {
        ProductoResponse response = obtenerProductoPorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<ProductoResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerProductosRequest request = ObtenerProductosRequest.createFromRequest(params);
        PaginatedResponse<List<ProductoResponse>> response = obtenerProductosUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @RequiereRol("Administrador")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarProductoRequest request = ActualizarProductoRequest.createFromRequest(body);
        ProductoResponse response = actualizarProductoUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @RequiereRol("Administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarProductoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // Situacion de inventario y disponibilidad (inventario.md INV-30, INV-31, INV-46).
    @GetMapping("/{id}/inventario")
    public ResponseEntity<InventarioProductoResponse> obtenerInventario(@PathVariable String id) {
        return ResponseEntity.ok(obtenerInventarioProductoUseCase.execute(id));
    }
}
