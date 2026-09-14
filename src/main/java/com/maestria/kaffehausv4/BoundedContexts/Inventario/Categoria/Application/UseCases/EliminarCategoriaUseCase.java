package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarCategoriaUseCase {
    private final ICategoriaRepository categoriaRepository;

    public EliminarCategoriaUseCase(ICategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public void execute(String categoriaId) {
        CategoriaId id = new CategoriaId(categoriaId);

        categoriaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId + "' no existe"));

        categoriaRepository.eliminar(id);
    }
}
