package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Datasources.IRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class RecetaDatasourceImpl implements IRecetaDatasource {

    private final RecetaJpaRepository recetaJpaRepository;

    public RecetaDatasourceImpl(RecetaJpaRepository recetaJpaRepository) {
        this.recetaJpaRepository = recetaJpaRepository;
    }

    @Override
    public Receta crear(Receta receta) {
        RecetaJpaModel model = RecetaJpaMapper.toPersistence(receta);
        return RecetaJpaMapper.toDomain(recetaJpaRepository.save(model));
    }

    @Override
    public Receta actualizar(RecetaId recetaId, Receta receta) {
        RecetaJpaModel model = recetaJpaRepository.findByIdAndBorradoFalse(aUuid(recetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId.getValue() + "' no existe"));

        RecetaJpaMapper.copiarCambios(receta, model);

        return RecetaJpaMapper.toDomain(recetaJpaRepository.save(model));
    }

    @Override
    public Optional<Receta> obtenerPorId(RecetaId recetaId) {
        return recetaJpaRepository.findByIdAndBorradoFalse(aUuid(recetaId.getValue()))
                .map(RecetaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Receta>> obtenerTodos(ObtenerRecetasRequest obtenerRecetasRequest) {
        int skip = obtenerRecetasRequest.getSkip();
        int limit = obtenerRecetasRequest.getLimit();
        String search = obtenerRecetasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<RecetaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? recetaJpaRepository.findByBorradoFalse(pageable)
                : recetaJpaRepository.findByBorradoFalseAndTituloContainingIgnoreCase(search.trim(), pageable);

        List<Receta> datos = pagina.getContent().stream()
                .map(RecetaJpaMapper::toDomain)
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
    public void eliminar(RecetaId recetaId) {
        RecetaJpaModel model = recetaJpaRepository.findByIdAndBorradoFalse(aUuid(recetaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        recetaJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
