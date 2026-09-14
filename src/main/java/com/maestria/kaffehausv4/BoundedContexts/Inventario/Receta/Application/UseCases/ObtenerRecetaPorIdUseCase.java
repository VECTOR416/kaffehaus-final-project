package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerRecetaPorIdUseCase {
    private final IRecetaRepository recetaRepository;

    public ObtenerRecetaPorIdUseCase(
            IRecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    public RecetaResponse execute(String recetaId) {
        RecetaId id = new RecetaId(recetaId);

        Receta receta = recetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId + "' no existe"));

        return RecetaResponse.createFromAggregate(receta);
    }
}
