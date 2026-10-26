package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Asocia el cliente a una venta ya cobrada (ventas.md VEN-31). Es la unica correccion que
 * admite una venta: lo demas ya movio inventario (VEN-30). Una venta anulada no cambia (VEN-32).
 */
@Service
@Transactional
public class ActualizarVentaUseCase {
    // Fila de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_ANULADA = "Anulada";

    private final IVentaRepository ventaRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;

    public ActualizarVentaUseCase(
            IVentaRepository ventaRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public VentaResponse execute(String ventaId, ActualizarVentaRequest actualizarVentaRequest) {
        VentaId id = new VentaId(ventaId);

        Venta venta = ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId + "' no existe"));

        ClienteId clienteId = new ClienteId(actualizarVentaRequest.getIdCliente());
        clienteRepository.obtenerPorId(clienteId)
                .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + actualizarVentaRequest.getIdCliente() + "' no existe"));
        EstadoVenta anulada = estadoVentaRepository.obtenerPorNombre(ESTADO_ANULADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_ANULADA + "' no esta registrado"));

        venta.asignarCliente(clienteId, anulada.getId());

        Venta ventaActualizada = ventaRepository.actualizar(id, venta);

        return toResponse(ventaActualizada);
    }

    private VentaResponse toResponse(Venta venta) {
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));
        return VentaResponse.createFromAggregate(venta, estadoVenta);
    }
}
