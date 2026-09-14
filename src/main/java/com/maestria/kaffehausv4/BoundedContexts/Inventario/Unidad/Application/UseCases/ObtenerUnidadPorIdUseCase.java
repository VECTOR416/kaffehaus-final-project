package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerUnidadPorIdUseCase {
    private final IUnidadRepository unidadRepository;

    public ObtenerUnidadPorIdUseCase(
            IUnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public UnidadResponse execute(String unidadId) {
        UnidadId id = new UnidadId(unidadId);

        Unidad unidad = unidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId + "' no existe"));

        return UnidadResponse.createFromAggregate(unidad);
    }
}
