package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Datasources.ICategoriaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class CategoriaDatasourceImpl implements ICategoriaDatasource {

    private final CategoriaJpaRepository categoriaJpaRepository;

    public CategoriaDatasourceImpl(CategoriaJpaRepository categoriaJpaRepository) {
        this.categoriaJpaRepository = categoriaJpaRepository;
    }

    @Override
    public Categoria crear(Categoria categoria) {
        CategoriaJpaModel model = CategoriaJpaMapper.toPersistence(categoria);
        return CategoriaJpaMapper.toDomain(categoriaJpaRepository.save(model));
    }

    @Override
    public Categoria actualizar(CategoriaId categoriaId, Categoria categoria) {
        CategoriaJpaModel model = categoriaJpaRepository.findByIdAndBorradoFalse(aUuid(categoriaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId.getValue() + "' no existe"));

        CategoriaJpaMapper.copiarCambios(categoria, model);

        return CategoriaJpaMapper.toDomain(categoriaJpaRepository.save(model));
    }

    @Override
    public Optional<Categoria> obtenerPorId(CategoriaId categoriaId) {
        return categoriaJpaRepository.findByIdAndBorradoFalse(aUuid(categoriaId.getValue()))
                .map(CategoriaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Categoria>> obtenerTodos(ObtenerCategoriasRequest obtenerCategoriasRequest) {
        int skip = obtenerCategoriasRequest.getSkip();
        int limit = obtenerCategoriasRequest.getLimit();
        String search = obtenerCategoriasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<CategoriaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? categoriaJpaRepository.findByBorradoFalse(pageable)
                : categoriaJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<Categoria> datos = pagina.getContent().stream()
                .map(CategoriaJpaMapper::toDomain)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                datos,
                skip,
                limit,
                pagina.getTotalElements(),
                skip + datos.size() < pagina.getTotalElements()
        );
    }

    @Override
    public void eliminar(CategoriaId categoriaId) {
        CategoriaJpaModel model = categoriaJpaRepository.findByIdAndBorradoFalse(aUuid(categoriaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId.getValue() + "' no existe"));

        Categoria categoria = CategoriaJpaMapper.toDomain(model);
        categoria.eliminar();

        CategoriaJpaMapper.copiarCambios(categoria, model);
        categoriaJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
