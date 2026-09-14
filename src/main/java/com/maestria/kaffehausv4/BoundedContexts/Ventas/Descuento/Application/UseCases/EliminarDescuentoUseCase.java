package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarDescuentoUseCase {
    private final IDescuentoRepository descuentoRepository;

    public EliminarDescuentoUseCase(IDescuentoRepository descuentoRepository) {
        this.descuentoRepository = descuentoRepository;
    }

    public void execute(String descuentoId) {
        DescuentoId id = new DescuentoId(descuentoId);

        descuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId + "' no existe"));

        descuentoRepository.eliminar(id);
    }
}
