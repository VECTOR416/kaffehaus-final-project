package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarTipoDescuentoUseCase {
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public EliminarTipoDescuentoUseCase(ITipoDescuentoRepository tipoDescuentoRepository) {
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public void execute(String tipoDescuentoId) {
        TipoDescuentoId id = new TipoDescuentoId(tipoDescuentoId);

        tipoDescuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + tipoDescuentoId + "' no existe"));

        tipoDescuentoRepository.eliminar(id);
    }
}
