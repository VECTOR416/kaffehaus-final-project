package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ActualizarUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarUnidadUseCase {
    private final IUnidadRepository unidadRepository;

    public ActualizarUnidadUseCase(
            IUnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public UnidadResponse execute(String unidadId, ActualizarUnidadRequest actualizarUnidadRequest) {
        UnidadId id = new UnidadId(unidadId);

        Unidad unidad = unidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId + "' no existe"));

        unidad.actualizar(
                actualizarUnidadRequest.getUnidad(),
                actualizarUnidadRequest.getAbreviatura(),
                actualizarUnidadRequest.getEsBorrable());

        Unidad unidadActualizada = unidadRepository.actualizar(id, unidad);

        return UnidadResponse.createFromAggregate(unidadActualizada);
    }
}
