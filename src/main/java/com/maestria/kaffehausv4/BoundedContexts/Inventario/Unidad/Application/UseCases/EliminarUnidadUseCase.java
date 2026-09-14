package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarUnidadUseCase {
    private final IUnidadRepository unidadRepository;

    public EliminarUnidadUseCase(IUnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public void execute(String unidadId) {
        UnidadId id = new UnidadId(unidadId);

        unidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId + "' no existe"));

        unidadRepository.eliminar(id);
    }
}
