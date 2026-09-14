package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ActualizarEstadoVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarEstadoVentaUseCase {
    private final IEstadoVentaRepository estadoVentaRepository;

    public ActualizarEstadoVentaUseCase(
            IEstadoVentaRepository estadoVentaRepository) {
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public EstadoVentaResponse execute(String estadoVentaId, ActualizarEstadoVentaRequest actualizarEstadoVentaRequest) {
        EstadoVentaId id = new EstadoVentaId(estadoVentaId);

        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + estadoVentaId + "' no existe"));

        estadoVenta.actualizar(
                actualizarEstadoVentaRequest.getNombre());

        EstadoVenta estadoVentaActualizado = estadoVentaRepository.actualizar(id, estadoVenta);

        return EstadoVentaResponse.createFromAggregate(estadoVentaActualizado);
    }
}
