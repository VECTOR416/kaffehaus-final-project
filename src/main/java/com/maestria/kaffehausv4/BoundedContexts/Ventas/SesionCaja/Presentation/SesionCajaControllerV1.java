package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Presentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.AbrirSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.EstadoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.AbrirSesionCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.CerrarSesionCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.ObtenerEstadoCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.ObtenerSesionCajaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.ObtenerSesionesCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.RegistrarMovimientoCajaUseCase;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;
import com.maestria.kaffehausv4.Shared.Presentation.Security.UsuarioEnSesion;

// Todo el recurso exige sesion con rol Operador o Administrador. Quien puede cerrar la caja
// (su titular o un Administrador) lo decide el agregado, porque depende del turno.
// La caja no tiene alta, edicion ni baja genericas: se abre, se le registran movimientos y se
// cierra (arquitectura.md 6.5, arquitectura.md 4.11).
@RequiereRol({"Operador", "Administrador"})
@RestController
@RequestMapping("api/v1/sesion-caja")
public class SesionCajaControllerV1 {

    private final AbrirSesionCajaUseCase abrirSesionCajaUseCase;
    private final RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    private final CerrarSesionCajaUseCase cerrarSesionCajaUseCase;
    private final ObtenerEstadoCajaUseCase obtenerEstadoCajaUseCase;
    private final ObtenerSesionCajaPorIdUseCase obtenerSesionCajaPorIdUseCase;
    private final ObtenerSesionesCajaUseCase obtenerSesionesCajaUseCase;

    public SesionCajaControllerV1(
            AbrirSesionCajaUseCase abrirSesionCajaUseCase,
            RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase,
            CerrarSesionCajaUseCase cerrarSesionCajaUseCase,
            ObtenerEstadoCajaUseCase obtenerEstadoCajaUseCase,
            ObtenerSesionCajaPorIdUseCase obtenerSesionCajaPorIdUseCase,
            ObtenerSesionesCajaUseCase obtenerSesionesCajaUseCase) {
        this.abrirSesionCajaUseCase = abrirSesionCajaUseCase;
        this.registrarMovimientoCajaUseCase = registrarMovimientoCajaUseCase;
        this.cerrarSesionCajaUseCase = cerrarSesionCajaUseCase;
        this.obtenerEstadoCajaUseCase = obtenerEstadoCajaUseCase;
        this.obtenerSesionCajaPorIdUseCase = obtenerSesionCajaPorIdUseCase;
        this.obtenerSesionesCajaUseCase = obtenerSesionesCajaUseCase;
    }

    @PostMapping("/apertura")
    public ResponseEntity<SesionCajaResponse> abrir(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        AbrirSesionCajaRequest request = AbrirSesionCajaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(abrirSesionCajaUseCase.execute(request));
    }

    @PostMapping("/movimientos")
    public ResponseEntity<SesionCajaResponse> registrarMovimiento(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        RegistrarMovimientoCajaRequest request = RegistrarMovimientoCajaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarMovimientoCajaUseCase.execute(request));
    }

    @PostMapping("/cierre")
    public ResponseEntity<ArqueoCajaResponse> cerrar(@RequestBody Map<String, Object> body, @UsuarioEnSesion String idUsuario) {
        CerrarSesionCajaRequest request = CerrarSesionCajaRequest.createFromRequest(body, idUsuario);
        return ResponseEntity.ok(cerrarSesionCajaUseCase.execute(request));
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoCajaResponse> obtenerEstado() {
        return ResponseEntity.ok(obtenerEstadoCajaUseCase.execute());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SesionCajaResponse> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(obtenerSesionCajaPorIdUseCase.execute(id));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<List<SesionCajaResponse>>> obtenerTodos(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("search", search);

        ObtenerSesionesCajaRequest request = ObtenerSesionesCajaRequest.createFromRequest(params);
        return ResponseEntity.ok(obtenerSesionesCajaUseCase.execute(request));
    }
}
