package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Datasources.IUnidadDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class UnidadDatasourceImpl implements IUnidadDatasource {

    private final UnidadJpaRepository unidadJpaRepository;

    public UnidadDatasourceImpl(UnidadJpaRepository unidadJpaRepository) {
        this.unidadJpaRepository = unidadJpaRepository;
    }

    @Override
    public Unidad crear(Unidad unidad) {
        UnidadJpaModel model = UnidadJpaMapper.toPersistence(unidad);
        return UnidadJpaMapper.toDomain(unidadJpaRepository.save(model));
    }

    @Override
    public Unidad actualizar(UnidadId unidadId, Unidad unidad) {
        UnidadJpaModel model = unidadJpaRepository.findByIdAndBorradoFalse(aUuid(unidadId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId.getValue() + "' no existe"));

        UnidadJpaMapper.copiarCambios(unidad, model);

        return UnidadJpaMapper.toDomain(unidadJpaRepository.save(model));
    }

    @Override
    public Optional<Unidad> obtenerPorId(UnidadId unidadId) {
        return unidadJpaRepository.findByIdAndBorradoFalse(aUuid(unidadId.getValue()))
                .map(UnidadJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Unidad>> obtenerTodos(ObtenerUnidadesRequest obtenerUnidadesRequest) {
        int skip = obtenerUnidadesRequest.getSkip();
        int limit = obtenerUnidadesRequest.getLimit();
        String search = obtenerUnidadesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<UnidadJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? unidadJpaRepository.findByBorradoFalse(pageable)
                : unidadJpaRepository.findByBorradoFalseAndUnidadContainingIgnoreCase(search.trim(), pageable);

        List<Unidad> datos = pagina.getContent().stream()
                .map(UnidadJpaMapper::toDomain)
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
    public void eliminar(UnidadId unidadId) {
        UnidadJpaModel model = unidadJpaRepository.findByIdAndBorradoFalse(aUuid(unidadId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        unidadJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
