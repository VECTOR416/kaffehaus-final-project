package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarLoteUseCase {
    private final ILoteRepository loteRepository;

    public EliminarLoteUseCase(ILoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    public void execute(String loteId) {
        LoteId id = new LoteId(loteId);

        loteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        loteRepository.eliminar(id);
    }
}
