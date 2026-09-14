package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerPasoRecetaPorIdUseCase {
    private final IPasoRecetaRepository pasoRecetaRepository;

    public ObtenerPasoRecetaPorIdUseCase(
            IPasoRecetaRepository pasoRecetaRepository) {
        this.pasoRecetaRepository = pasoRecetaRepository;
    }

    public PasoRecetaResponse execute(String pasoRecetaId) {
        PasoRecetaId id = new PasoRecetaId(pasoRecetaId);

        PasoReceta pasoReceta = pasoRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El paso de receta con id '" + pasoRecetaId + "' no existe"));

        return PasoRecetaResponse.createFromAggregate(pasoReceta);
    }
}
