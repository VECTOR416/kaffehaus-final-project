package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerCategoriasUseCase {
    private final ICategoriaRepository categoriaRepository;

    public ObtenerCategoriasUseCase(
            ICategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public PaginatedResponse<List<CategoriaResponse>> execute(ObtenerCategoriasRequest obtenerCategoriasRequest) {
        PaginatedResponse<List<Categoria>> resultado = categoriaRepository.obtenerTodos(obtenerCategoriasRequest);

        List<CategoriaResponse> categorias = resultado.getData().stream()
                .map(CategoriaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                categorias,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
