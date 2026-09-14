package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ActualizarPasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarPasoRecetaUseCase {
    private final IPasoRecetaRepository pasoRecetaRepository;
    private final IRecetaRepository recetaRepository;

    public ActualizarPasoRecetaUseCase(
            IPasoRecetaRepository pasoRecetaRepository,
            IRecetaRepository recetaRepository) {
        this.pasoRecetaRepository = pasoRecetaRepository;
        this.recetaRepository = recetaRepository;
    }

    public PasoRecetaResponse execute(String pasoRecetaId, ActualizarPasoRecetaRequest actualizarPasoRecetaRequest) {
        PasoRecetaId id = new PasoRecetaId(pasoRecetaId);

        PasoReceta pasoReceta = pasoRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El paso de receta con id '" + pasoRecetaId + "' no existe"));

        RecetaId recetaId = null;
        if (actualizarPasoRecetaRequest.getIdReceta() != null) {
            recetaId = new RecetaId(actualizarPasoRecetaRequest.getIdReceta());
            recetaRepository.obtenerPorId(recetaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + actualizarPasoRecetaRequest.getIdReceta() + "' no existe"));
        }

        pasoReceta.actualizar(
                actualizarPasoRecetaRequest.getPasos(),
                actualizarPasoRecetaRequest.getDescripcion(),
                recetaId);

        PasoReceta pasoRecetaActualizado = pasoRecetaRepository.actualizar(id, pasoReceta);

        return PasoRecetaResponse.createFromAggregate(pasoRecetaActualizado);
    }
}
