package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ActualizarCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarCategoriaUseCase {
    private final ICategoriaRepository categoriaRepository;

    public ActualizarCategoriaUseCase(
            ICategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CategoriaResponse execute(String categoriaId, ActualizarCategoriaRequest actualizarCategoriaRequest) {
        CategoriaId id = new CategoriaId(categoriaId);

        Categoria categoria = categoriaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId + "' no existe"));

        categoria.actualizar(
                actualizarCategoriaRequest.getNombre(),
                actualizarCategoriaRequest.getEsBorrable());

        Categoria categoriaActualizada = categoriaRepository.actualizar(id, categoria);

        return CategoriaResponse.createFromAggregate(categoriaActualizada);
    }
}
