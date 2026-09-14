package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarDetalleVentaUseCase {
    private final IDetalleVentaRepository detalleVentaRepository;

    public EliminarDetalleVentaUseCase(IDetalleVentaRepository detalleVentaRepository) {
        this.detalleVentaRepository = detalleVentaRepository;
    }

    public void execute(String detalleVentaId) {
        DetalleVentaId id = new DetalleVentaId(detalleVentaId);

        detalleVentaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de venta con id '" + detalleVentaId + "' no existe"));

        detalleVentaRepository.eliminar(id);
    }
}
