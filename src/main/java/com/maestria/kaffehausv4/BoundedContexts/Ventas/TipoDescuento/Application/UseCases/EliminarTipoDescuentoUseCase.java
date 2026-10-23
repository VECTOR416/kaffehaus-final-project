package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de el tipo de descuento. No se da de baja si un descuento vivo lo usa: el descuento
 * dejaria de poder calcularse (logica.md GENERAL-18). Los tipos semanticos tampoco (GENERAL-19, en
 * el agregado). Solo Administrador (GENERAL-17).
 */
@Service
@Transactional
public class EliminarTipoDescuentoUseCase {
    private final ITipoDescuentoRepository tipoDescuentoRepository;
    private final IDescuentoRepository descuentoRepository;

    public EliminarTipoDescuentoUseCase(
            ITipoDescuentoRepository tipoDescuentoRepository,
            IDescuentoRepository descuentoRepository) {
        this.tipoDescuentoRepository = tipoDescuentoRepository;
        this.descuentoRepository = descuentoRepository;
    }

    public void execute(String tipoDescuentoId) {
        TipoDescuentoId id = new TipoDescuentoId(tipoDescuentoId);

        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + tipoDescuentoId + "' no existe"));
        if (descuentoRepository.existeConTipoDescuento(id)) {
            throw DomainException.throwException(409,
                    "El tipo de descuento '" + tipoDescuento.getNombre() + "' lo usan descuentos vivos y no se puede dar de baja");
        }

        tipoDescuentoRepository.eliminar(id);
    }
}
