package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarReintegroDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: anula una venta (ventas.md VEN-40..VEN-44). No tiene endpoint; la invocan
 * AnularVentaUseCase y la cancelacion de un pedido (VEN-52), dentro de su transaccion.
 *
 * La venta pasa a 'Anulada' con su motivo y quien la anulo; lineas, importes, costos y
 * movimientos se conservan. Sin reintegro el inventario no cambia (VEN-41). Con reintegro, cada
 * salida de la venta recibe su compensatorio de entrada (VEN-42); si una parte no se
 * puede reintegrar, la anulacion entera falla y la venta sigue 'Completada' (VEN-43).
 */
@Service
@Transactional
public class RegistrarAnulacionDeVentaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_COMPLETADA = "Completada";
    private static final String ESTADO_ANULADA = "Anulada";

    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase;

    public RegistrarAnulacionDeVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IEstadoVentaRepository estadoVentaRepository,
            RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoVentaRepository = estadoVentaRepository;
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

        if (request.isReintegrar()) {
            registrarReintegroDeInventarioUseCase.execute(RegistrarReintegroDeInventarioRequest.crear(
                    ventaAnulada.motivoDeMovimientos(),
                    "Anulacion de la venta '" + ventaAnulada.getId().getValue() + "'",
                    request.getIdUsuario(),
                    request.getFecha()));
        }

        return VentaResponse.createFromAggregate(ventaAnulada, anulada);
    }

    private EstadoVenta estado(String nombre) {
        return estadoVentaRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + nombre + "' no esta registrado"));
    }
}
