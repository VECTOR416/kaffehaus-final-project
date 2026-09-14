package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ActualizarProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarProduccionUseCase {
    private final IProduccionRepository produccionRepository;
    private final IRecetaRepository recetaRepository;

    public ActualizarProduccionUseCase(
            IProduccionRepository produccionRepository,
            IRecetaRepository recetaRepository) {
        this.produccionRepository = produccionRepository;
        this.recetaRepository = recetaRepository;
    }

    public ProduccionResponse execute(String produccionId, ActualizarProduccionRequest actualizarProduccionRequest) {
        ProduccionId id = new ProduccionId(produccionId);

        Produccion produccion = produccionRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La produccion con id '" + produccionId + "' no existe"));

        RecetaId recetaId = null;
        if (actualizarProduccionRequest.getIdReceta() != null) {
            recetaId = new RecetaId(actualizarProduccionRequest.getIdReceta());
            recetaRepository.obtenerPorId(recetaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + actualizarProduccionRequest.getIdReceta() + "' no existe"));
        }

        produccion.actualizar(
                actualizarProduccionRequest.getCantidadEsperada(),
                actualizarProduccionRequest.getCantidadReal(),
                actualizarProduccionRequest.getDiferencia(),
                actualizarProduccionRequest.getMotivoDiferencia(),
                actualizarProduccionRequest.getFechaProduccion(),
                recetaId);

        Produccion produccionActualizada = produccionRepository.actualizar(id, produccion);

        return ProduccionResponse.createFromAggregate(produccionActualizada);
    }
}
