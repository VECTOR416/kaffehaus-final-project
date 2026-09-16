package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Datasources.IIngredienteRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ObtenerIngredientesRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class IngredienteRecetaDatasourceImpl implements IIngredienteRecetaDatasource {

    private final IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository;

    public IngredienteRecetaDatasourceImpl(IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository) {
        this.ingredienteRecetaJpaRepository = ingredienteRecetaJpaRepository;
    }

    @Override
    public IngredienteReceta crear(IngredienteReceta ingredienteReceta) {
        IngredienteRecetaJpaModel model = IngredienteRecetaJpaMapper.toPersistence(ingredienteReceta);
        return IngredienteRecetaJpaMapper.toDomain(ingredienteRecetaJpaRepository.save(model));
    }

    @Override
    public IngredienteReceta actualizar(IngredienteRecetaId ingredienteRecetaId, IngredienteReceta ingredienteReceta) {
        IngredienteRecetaJpaModel model = ingredienteRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(ingredienteRecetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El ingrediente de receta con id '" + ingredienteRecetaId.getValue() + "' no existe"));

        IngredienteRecetaJpaMapper.copiarCambios(ingredienteReceta, model);

        return IngredienteRecetaJpaMapper.toDomain(ingredienteRecetaJpaRepository.save(model));
    }

    @Override
    public Optional<IngredienteReceta> obtenerPorId(IngredienteRecetaId ingredienteRecetaId) {
        return ingredienteRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(ingredienteRecetaId.getValue()))
                .map(IngredienteRecetaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<IngredienteReceta>> obtenerTodos(ObtenerIngredientesRecetaRequest obtenerIngredientesRecetaRequest) {
        int skip = obtenerIngredientesRecetaRequest.getSkip();
        int limit = obtenerIngredientesRecetaRequest.getLimit();
        //String search = obtenerIngredientesRecetaRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<IngredienteRecetaJpaModel> pagina = ingredienteRecetaJpaRepository.findByBorradoFalse(pageable);

        List<IngredienteReceta> datos = pagina.getContent().stream()
                .map(IngredienteRecetaJpaMapper::toDomain)
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
    public void eliminar(IngredienteRecetaId ingredienteRecetaId) {
        IngredienteRecetaJpaModel model = ingredienteRecetaJpaRepository.findByIdAndBorradoFalse(aUuid(ingredienteRecetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El ingrediente de receta con id '" + ingredienteRecetaId.getValue() + "' no existe"));

        IngredienteReceta ingredienteReceta = IngredienteRecetaJpaMapper.toDomain(model);
        ingredienteReceta.eliminar();

        IngredienteRecetaJpaMapper.copiarCambios(ingredienteReceta, model);
        ingredienteRecetaJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
