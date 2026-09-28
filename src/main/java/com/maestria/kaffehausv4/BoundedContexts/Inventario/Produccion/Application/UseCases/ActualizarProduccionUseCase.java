package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ActualizarProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarProduccionUseCase {
    private final IProduccionRepository produccionRepository;

    public ActualizarProduccionUseCase(IProduccionRepository produccionRepository) {
        this.produccionRepository = produccionRepository;
    }

    public ProduccionResponse execute(String produccionId, ActualizarProduccionRequest actualizarProduccionRequest) {
        ProduccionId id = new ProduccionId(produccionId);

        Produccion produccion = produccionRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La produccion con id '" + produccionId + "' no existe"));

        produccion.actualizar(
                actualizarProduccionRequest.getMotivoDiferencia(),
                actualizarProduccionRequest.getFechaProduccion());

        Produccion produccionActualizada = produccionRepository.actualizar(id, produccion);

        return ProduccionResponse.createFromAggregate(produccionActualizada);
    }
}
