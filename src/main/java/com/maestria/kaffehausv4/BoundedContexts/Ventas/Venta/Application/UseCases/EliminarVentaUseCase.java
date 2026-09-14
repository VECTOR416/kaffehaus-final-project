package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarVentaUseCase {
    private final IVentaRepository ventaRepository;

    public EliminarVentaUseCase(IVentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public void execute(String ventaId) {
        VentaId id = new VentaId(ventaId);

        ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId + "' no existe"));

        ventaRepository.eliminar(id);
    }
}
