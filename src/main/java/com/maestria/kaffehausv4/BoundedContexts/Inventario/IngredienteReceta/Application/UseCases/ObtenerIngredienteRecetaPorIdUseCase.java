package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.IngredienteRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerIngredienteRecetaPorIdUseCase {
    private final IIngredienteRecetaRepository ingredienteRecetaRepository;

    public ObtenerIngredienteRecetaPorIdUseCase(
            IIngredienteRecetaRepository ingredienteRecetaRepository) {
        this.ingredienteRecetaRepository = ingredienteRecetaRepository;
    }

    public IngredienteRecetaResponse execute(String ingredienteRecetaId) {
        IngredienteRecetaId id = new IngredienteRecetaId(ingredienteRecetaId);

        IngredienteReceta ingredienteReceta = ingredienteRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El ingrediente de receta con id '" + ingredienteRecetaId + "' no existe"));

        return IngredienteRecetaResponse.createFromAggregate(ingredienteReceta);
    }
}
