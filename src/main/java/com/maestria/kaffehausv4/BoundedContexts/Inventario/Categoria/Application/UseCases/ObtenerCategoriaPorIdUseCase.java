package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerCategoriaPorIdUseCase {
    private final ICategoriaRepository categoriaRepository;

    public ObtenerCategoriaPorIdUseCase(
            ICategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CategoriaResponse execute(String categoriaId) {
        CategoriaId id = new CategoriaId(categoriaId);

        Categoria categoria = categoriaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId + "' no existe"));

        return CategoriaResponse.createFromAggregate(categoria);
    }
}
