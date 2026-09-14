package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.DetalleVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerDetalleVentaPorIdUseCase {
    private final IDetalleVentaRepository detalleVentaRepository;

    public ObtenerDetalleVentaPorIdUseCase(
            IDetalleVentaRepository detalleVentaRepository) {
        this.detalleVentaRepository = detalleVentaRepository;
    }

    public DetalleVentaResponse execute(String detalleVentaId) {
        DetalleVentaId id = new DetalleVentaId(detalleVentaId);

        DetalleVenta detalleVenta = detalleVentaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de venta con id '" + detalleVentaId + "' no existe"));

        return DetalleVentaResponse.createFromAggregate(detalleVenta);
    }
}
