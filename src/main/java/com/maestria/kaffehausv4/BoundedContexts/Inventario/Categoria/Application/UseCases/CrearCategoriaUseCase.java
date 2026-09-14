package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CrearCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;

@Service
@Transactional
public class CrearCategoriaUseCase {
    private final ICategoriaRepository categoriaRepository;

    public CrearCategoriaUseCase(
            ICategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CategoriaResponse execute(CrearCategoriaRequest crearCategoriaRequest) {
        Categoria categoria = Categoria.crear(
                crearCategoriaRequest.getNombre());

        Categoria categoriaCreada = categoriaRepository.crear(categoria);

        return CategoriaResponse.createFromAggregate(categoriaCreada);
    }
}
