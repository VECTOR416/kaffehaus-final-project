package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerDetalleLotePorIdUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;

    public ObtenerDetalleLotePorIdUseCase(
            IDetalleLoteRepository detalleLoteRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
    }

    public DetalleLoteResponse execute(String detalleLoteId) {
        DetalleLoteId id = new DetalleLoteId(detalleLoteId);

        DetalleLote detalleLote = detalleLoteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + detalleLoteId + "' no existe"));

        return DetalleLoteResponse.createFromAggregate(detalleLote);
    }
}
