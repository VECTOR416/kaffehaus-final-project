package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarReintegroDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: anula una venta (logica.md VENTA-11). No tiene endpoint; la invocan
 * AnularVentaUseCase y la cancelacion de un pedido (PEDIDO-05), dentro de su transaccion.
 *
 * 1. La venta pasa a 'Anulada' con su motivo y quien la anulo; lineas, importes y pagos se
 *    conservan.
 * 2. Devolucion del efectivo (VENTA-27): si la venta es de la caja abierta no se registra nada
 *    (su arqueo ya la excluye, SESIONCAJA-06); si es de un turno ya cerrado, la caja abierta
 *    registra un egreso por lo cobrado en efectivo, con la venta (SESIONCAJA-13). Sin caja
 *    abierta, o si el egreso la dejaria en negativo, 409.
 * 3. Con reintegro, devuelve lo que desconto la venta a sus mismos sujetos (VENTA-25); sin
 *    reintegro el inventario no cambia.
 *
 * La caja se bloquea antes que el inventario (GENERAL-15). Si una parte falla, la anulacion
 * entera revierte y la venta sigue 'Completada'.
 */
@Service
@Transactional
public class RegistrarAnulacionDeVentaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (logica.md GENERAL-04).
    private static final String ESTADO_COMPLETADA = "Completada";
    private static final String ESTADO_ANULADA = "Anulada";

    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final ISesionCajaRepository sesionCajaRepository;
    private final ITipoPagoRepository tipoPagoRepository;
    private final RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase;

    public RegistrarAnulacionDeVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IEstadoVentaRepository estadoVentaRepository,
            ISesionCajaRepository sesionCajaRepository,
            ITipoPagoRepository tipoPagoRepository,
            RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.sesionCajaRepository = sesionCajaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.registrarReintegroDeInventarioUseCase = registrarReintegroDeInventarioUseCase;
    }

    public VentaResponse execute(RegistrarAnulacionDeVentaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        VentaId id = new VentaId(request.getIdVenta());
        Venta venta = ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + request.getIdVenta() + "' no existe"));

        EstadoVenta completada = estado(ESTADO_COMPLETADA);
        EstadoVenta anulada = estado(ESTADO_ANULADA);

        venta.anular(completada.getId(), anulada.getId(), usuarioId, request.getMotivo());
        Venta ventaAnulada = ventaRepository.actualizar(id, venta);

        devolverEfectivo(ventaAnulada, usuarioId, completada);

        if (request.isReintegrar()) {
            registrarReintegroDeInventarioUseCase.execute(RegistrarReintegroDeInventarioRequest.crear(
                    ventaAnulada.getId().getValue(),
                    usuarioId.getValue(),
                    "Reintegro del ticket " + ventaAnulada.getNumeroTicket(),
                    request.getFecha()));
        }

        return VentaResponse.createFromAggregate(ventaAnulada, anulada);
    }

    /**
     * Lo cobrado en efectivo sale de la caja abierta solo si la venta es de un turno ya cerrado
     * (logica.md VENTA-27). Cuenta el monto de los pagos, no lo recibido: el cambio ya salio.
     */
    private void devolverEfectivo(Venta venta, UsuarioId usuarioId, EstadoVenta completada) {
        Set<String> tiposEfectivo = tiposEfectivo();
        BigDecimal efectivo = venta.getPagosActivos().stream()
                .filter(pago -> tiposEfectivo.contains(pago.getTipoPagoId().getValue()))
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (efectivo.signum() == 0) {
            return;
        }

        boolean deCajaAbierta = sesionCajaRepository.obtenerPorId(venta.getSesionCajaId())
                .map(SesionCaja::estaAbierta)
                .orElse(false);
        if (deCajaAbierta) {
            return;
        }

        SesionCaja abierta = sesionCajaRepository.obtenerAbiertaParaActualizar()
                .orElseThrow(() -> DomainException.throwException(409,
                        "La venta es de un turno ya cerrado; abre la caja para devolver el efectivo antes de anularla"));
        abierta.registrarDevolucion(usuarioId, efectivo, "Devolucion del ticket " + venta.getNumeroTicket(), venta.getId(),
                ventasEnEfectivo(abierta, completada, tiposEfectivo));
        sesionCajaRepository.actualizar(abierta.getId(), abierta);
    }

    /** Lo cobrado en efectivo en las ventas completadas de esa caja, para no dejarla en negativo. */
    private BigDecimal ventasEnEfectivo(SesionCaja sesionCaja, EstadoVenta completada, Set<String> tiposEfectivo) {
        return ventaRepository.obtenerPorSesionCaja(sesionCaja.getId()).stream()
                .filter(venta -> venta.estaEn(completada.getId()))
                .flatMap(venta -> venta.getPagosActivos().stream())
                .filter(pago -> tiposEfectivo.contains(pago.getTipoPagoId().getValue()))
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Set<String> tiposEfectivo() {
        return tipoPagoRepository.obtenerVivos().stream()
                .filter(TipoPago::esEfectivo)
                .map(tipo -> tipo.getId().getValue())
                .collect(Collectors.toSet());
    }

    private EstadoVenta estado(String nombre) {
        return estadoVentaRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + nombre + "' no esta registrado"));
    }
}
