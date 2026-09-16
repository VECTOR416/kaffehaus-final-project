package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.CrearPasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearPasoRecetaUseCase {
    private final IPasoRecetaRepository pasoRecetaRepository;
    private final IRecetaRepository recetaRepository;

    public CrearPasoRecetaUseCase(
            IPasoRecetaRepository pasoRecetaRepository,
            IRecetaRepository recetaRepository) {
        this.pasoRecetaRepository = pasoRecetaRepository;
        this.recetaRepository = recetaRepository;
    }

    public PasoRecetaResponse execute(CrearPasoRecetaRequest crearPasoRecetaRequest) {
        RecetaId recetaId = new RecetaId(crearPasoRecetaRequest.getIdReceta());
        recetaRepository.obtenerPorId(recetaId)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + crearPasoRecetaRequest.getIdReceta() + "' no existe"));

        PasoReceta pasoReceta = PasoReceta.crear(
                crearPasoRecetaRequest.getPasos(),
                crearPasoRecetaRequest.getDescripcion(),
                recetaId);

        PasoReceta pasoRecetaCreado = pasoRecetaRepository.crear(pasoReceta);

        return PasoRecetaResponse.createFromAggregate(pasoRecetaCreado);
    }
}
