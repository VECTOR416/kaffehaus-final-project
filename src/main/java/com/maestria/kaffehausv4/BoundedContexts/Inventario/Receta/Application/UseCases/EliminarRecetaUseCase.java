package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarRecetaUseCase {
    private final IRecetaRepository recetaRepository;

    public EliminarRecetaUseCase(IRecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    public void execute(String recetaId) {
        RecetaId id = new RecetaId(recetaId);

        recetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId + "' no existe"));

        recetaRepository.eliminar(id);
    }
}
