package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarDetalleLoteUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;

    public EliminarDetalleLoteUseCase(IDetalleLoteRepository detalleLoteRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
    }

    public void execute(String detalleLoteId) {
        DetalleLoteId id = new DetalleLoteId(detalleLoteId);

        detalleLoteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + detalleLoteId + "' no existe"));

        detalleLoteRepository.eliminar(id);
    }
}
