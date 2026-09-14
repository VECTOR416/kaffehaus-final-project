package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarEstadoVentaUseCase {
    private final IEstadoVentaRepository estadoVentaRepository;

    public EliminarEstadoVentaUseCase(IEstadoVentaRepository estadoVentaRepository) {
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public void execute(String estadoVentaId) {
        EstadoVentaId id = new EstadoVentaId(estadoVentaId);

        estadoVentaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + estadoVentaId + "' no existe"));

        estadoVentaRepository.eliminar(id);
    }
}
