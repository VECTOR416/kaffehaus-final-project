package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Se conserva para mantener el contrato de cinco operaciones del controller, pero una venta no
 * se da de baja (ventas.md VEN-33): el agregado lo rechaza con 409 y remite a la anulacion.
 * Si la venta no existe responde 404, como el resto del proyecto.
 */
@Service
@Transactional
public class EliminarVentaUseCase {
    private final IVentaRepository ventaRepository;

    public EliminarVentaUseCase(IVentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public void execute(String ventaId) {
        VentaId id = new VentaId(ventaId);

        Venta venta = ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId + "' no existe"));

        venta.eliminar();

        ventaRepository.eliminar(id);
    }
}
