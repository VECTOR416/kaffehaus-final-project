package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Datasources.IProduccionDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa.ProduccionJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa.ProduccionJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa.ProduccionJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class ProduccionDatasourceImpl implements IProduccionDatasource {

    private final ProduccionJpaRepository produccionJpaRepository;

    public ProduccionDatasourceImpl(ProduccionJpaRepository produccionJpaRepository) {
        this.produccionJpaRepository = produccionJpaRepository;
    }

    @Override
    public Produccion crear(Produccion produccion) {
        ProduccionJpaModel model = ProduccionJpaMapper.toPersistence(produccion);
        return ProduccionJpaMapper.toDomain(produccionJpaRepository.save(model));
    }

    @Override
    public Produccion actualizar(ProduccionId produccionId, Produccion produccion) {
        ProduccionJpaModel model = produccionJpaRepository.findByIdAndBorradoFalse(aUuid(produccionId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La produccion con id '" + produccionId.getValue() + "' no existe"));

        ProduccionJpaMapper.copiarCambios(produccion, model);

        return ProduccionJpaMapper.toDomain(produccionJpaRepository.save(model));
    }

    @Override
    public Optional<Produccion> obtenerPorId(ProduccionId produccionId) {
        return produccionJpaRepository.findByIdAndBorradoFalse(aUuid(produccionId.getValue()))
                .map(ProduccionJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Produccion>> obtenerTodos(ObtenerProduccionesRequest obtenerProduccionesRequest) {
        int skip = obtenerProduccionesRequest.getSkip();
        int limit = obtenerProduccionesRequest.getLimit();
        String search = obtenerProduccionesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ProduccionJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? produccionJpaRepository.findByBorradoFalse(pageable)
                : produccionJpaRepository.findByBorradoFalseAndMotivoDiferenciaContainingIgnoreCase(search.trim(), pageable);

        List<Produccion> datos = pagina.getContent().stream()
                .map(ProduccionJpaMapper::toDomain)
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
    public void eliminar(ProduccionId produccionId) {
        ProduccionJpaModel model = produccionJpaRepository.findByIdAndBorradoFalse(aUuid(produccionId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La produccion con id '" + produccionId.getValue() + "' no existe"));

        Produccion produccion = ProduccionJpaMapper.toDomain(model);
        produccion.eliminar();

        ProduccionJpaMapper.copiarCambios(produccion, model);
        produccionJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
