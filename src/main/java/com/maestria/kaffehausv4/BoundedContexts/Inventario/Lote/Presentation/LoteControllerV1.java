package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Presentation;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ActualizarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.CrearLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.EliminarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerLotePorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerLotesUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleAperturableResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.AbrirDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerDetallesAperturablesUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador; un metodo puede
// restringirlo mas con su propio @RequiereRol (GuardiaDeRoles).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/lote")
public class LoteControllerV1 {

    private final CrearLoteUseCase crearLoteUseCase;
    private final ActualizarLoteUseCase actualizarLoteUseCase;
    private final ObtenerLotePorIdUseCase obtenerLotePorIdUseCase;
    private final ObtenerLotesUseCase obtenerLotesUseCase;
    private final EliminarLoteUseCase eliminarLoteUseCase;
    private final ObtenerDetallesAperturablesUseCase obtenerDetallesAperturablesUseCase;
    private final AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;

    public LoteControllerV1(
            CrearLoteUseCase crearLoteUseCase,
            ActualizarLoteUseCase actualizarLoteUseCase,
            ObtenerLotePorIdUseCase obtenerLotePorIdUseCase,
            ObtenerLotesUseCase obtenerLotesUseCase,
            EliminarLoteUseCase eliminarLoteUseCase,
            ObtenerDetallesAperturablesUseCase obtenerDetallesAperturablesUseCase,
            AbrirDetalleLoteUseCase abrirDetalleLoteUseCase) {
        this.crearLoteUseCase = crearLoteUseCase;
        this.actualizarLoteUseCase = actualizarLoteUseCase;
        this.obtenerLotePorIdUseCase = obtenerLotePorIdUseCase;
        this.obtenerLotesUseCase = obtenerLotesUseCase;
        this.eliminarLoteUseCase = eliminarLoteUseCase;
        this.obtenerDetallesAperturablesUseCase = obtenerDetallesAperturablesUseCase;
        this.abrirDetalleLoteUseCase = abrirDetalleLoteUseCase;
    }

    @PostMapping
    public ResponseEntity<LoteResponse> crear(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        CrearLoteRequest request = CrearLoteRequest.createFromRequest(body, idUsuario);
        LoteResponse response = crearLoteUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponse> obtenerPorId(@PathVariable String id) {
        LoteResponse response = obtenerLotePorIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<LoteResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerLotesRequest request = ObtenerLotesRequest.createFromRequest(params);
        PaginatedResponse<List<LoteResponse>> response = obtenerLotesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @RequiereRol("Administrador")
    @PutMapping("/{id}")
    public ResponseEntity<LoteResponse> actualizar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        ActualizarLoteRequest request = ActualizarLoteRequest.createFromRequest(body);
        LoteResponse response = actualizarLoteUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarLoteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // Operaciones de inventario sobre las lineas del lote (inventario.md INV-13, INV-14).
    // Viven bajo la raiz porque detalle_lote no tiene endpoints propios (arquitectura.md 4.7).

    @GetMapping("/detalles-aperturables")
    public ResponseEntity<List<DetalleAperturableResponse>> obtenerDetallesAperturables(
            @RequestParam(required = false) String idProducto) {
        Map<String, Object> params = new HashMap<>();
        params.put("idProducto", idProducto);

        ObtenerDetallesAperturablesRequest request = ObtenerDetallesAperturablesRequest.createFromRequest(params);
        return ResponseEntity.ok(obtenerDetallesAperturablesUseCase.execute(request));
    }

    @PostMapping("/detalles/{idDetalleLote}/apertura")
    public ResponseEntity<ExistenciaResponse> abrirDetalle(@PathVariable String idDetalleLote, @RequestBody(required = false) Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        AbrirDetalleLoteRequest request = AbrirDetalleLoteRequest.createFromRequest(body != null ? body : Map.of(), idUsuario);
        ExistenciaResponse response = abrirDetalleLoteUseCase.execute(idDetalleLote, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
