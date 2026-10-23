package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Registra un ingreso o egreso menor de efectivo en la caja abierta (arquitectura.md 4.11, CAJ-21). La
 * sesion se bloquea en exclusiva, igual que al cerrarla, para que un movimiento no llegue
 * despues de calculado el arqueo.
 *
 * Un egreso no puede dejar el efectivo esperado en negativo (logica.md SESIONCAJA-10): por eso se
 * suma lo cobrado en efectivo en las ventas completadas de la sesion, igual que en el cierre.
 */
@Service
@Transactional
public class RegistrarMovimientoCajaUseCase {
    // Fila de catalogo que el flujo resuelve por nombre, igual que el resto del catalogo.
    private static final String ESTADO_COMPLETADA = "Completada";

    private final ISesionCajaRepository sesionCajaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IVentaRepository ventaRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final ITipoPagoRepository tipoPagoRepository;

    public RegistrarMovimientoCajaUseCase(
            ISesionCajaRepository sesionCajaRepository,
            IUsuarioRepository usuarioRepository,
            IVentaRepository ventaRepository,
            IEstadoVentaRepository estadoVentaRepository,
            ITipoPagoRepository tipoPagoRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ventaRepository = ventaRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public SesionCajaResponse execute(RegistrarMovimientoCajaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        SesionCaja sesionCaja = sesionCajaRepository.obtenerAbiertaParaActualizar()
                .orElseThrow(() -> DomainException.throwException(409, "No hay una caja abierta en la que registrar el movimiento"));

        sesionCaja.registrarMovimiento(usuarioId, request.getTipo(), request.getMonto(), request.getMotivo(), ventasEnEfectivo(sesionCaja));

        return SesionCajaResponse.createFromAggregate(sesionCajaRepository.actualizar(sesionCaja.getId(), sesionCaja));
    }

    /** Lo cobrado en efectivo en las ventas completadas de la sesion: el monto, no lo recibido. */
    private BigDecimal ventasEnEfectivo(SesionCaja sesionCaja) {
        EstadoVenta completada = estadoVentaRepository.obtenerPorNombre(ESTADO_COMPLETADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_COMPLETADA + "' no esta registrado"));
        Set<String> tiposEfectivo = tipoPagoRepository.obtenerVivos().stream()
                .filter(TipoPago::esEfectivo)
                .map(tipo -> tipo.getId().getValue())
                .collect(Collectors.toSet());
        return ventaRepository.obtenerPorSesionCaja(sesionCaja.getId()).stream()
                .filter(venta -> venta.estaEn(completada.getId()))
                .flatMap(venta -> venta.getPagosActivos().stream())
                .filter(pago -> tiposEfectivo.contains(pago.getTipoPagoId().getValue()))
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
