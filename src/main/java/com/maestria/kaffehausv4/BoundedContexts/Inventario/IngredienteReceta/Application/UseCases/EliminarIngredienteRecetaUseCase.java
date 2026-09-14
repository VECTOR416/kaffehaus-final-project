package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarIngredienteRecetaUseCase {
    private final IIngredienteRecetaRepository ingredienteRecetaRepository;

    public EliminarIngredienteRecetaUseCase(IIngredienteRecetaRepository ingredienteRecetaRepository) {
        this.ingredienteRecetaRepository = ingredienteRecetaRepository;
    }

    public void execute(String ingredienteRecetaId) {
        IngredienteRecetaId id = new IngredienteRecetaId(ingredienteRecetaId);

        ingredienteRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El ingrediente de receta con id '" + ingredienteRecetaId + "' no existe"));

        ingredienteRecetaRepository.eliminar(id);
    }
}
