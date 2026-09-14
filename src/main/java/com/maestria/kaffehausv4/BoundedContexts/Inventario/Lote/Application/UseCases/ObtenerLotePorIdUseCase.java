package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerLotePorIdUseCase {
    private final ILoteRepository loteRepository;

    public ObtenerLotePorIdUseCase(
            ILoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    public LoteResponse execute(String loteId) {
        LoteId id = new LoteId(loteId);

        Lote lote = loteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        return LoteResponse.createFromAggregate(lote);
    }
}
