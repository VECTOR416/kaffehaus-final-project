package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Datasource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Datasources.IEntidadDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa.EntidadJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa.EntidadJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa.EntidadesJpaModel;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class EntidadDatasourceImpl implements IEntidadDatasource{

    private final EntidadJpaRepository entidadJpaRepository;

    public EntidadDatasourceImpl(EntidadJpaRepository entidadJpaRepository) {
        this.entidadJpaRepository = entidadJpaRepository;
    }

    @Override
    public Entidad crear(Entidad entidad) {
        EntidadesJpaModel model = EntidadJpaMapper.toPersistence(entidad);
        return EntidadJpaMapper.toDomain(entidadJpaRepository.save(model));
    }

    @Override
    public Entidad actualizar(EntidadId entidadId, Entidad entidad) {
        EntidadesJpaModel model = entidadJpaRepository.findByIdAndBorradoFalse(aUuid(entidadId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + entidadId.getValue() + "' no existe"));

        EntidadJpaMapper.copiarCambios(entidad, model);

        return EntidadJpaMapper.toDomain(entidadJpaRepository.save(model));
    }

    @Override
    public Optional<Entidad> obtenerPorId(EntidadId entidadId) {
        return entidadJpaRepository.findByIdAndBorradoFalse(aUuid(entidadId.getValue()))
                .map(EntidadJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Entidad>> obtenerTodos(ObtenerEntidadesRequest obtenerEntidadesRequest) {
        int skip = obtenerEntidadesRequest.getSkip();
        int limit = obtenerEntidadesRequest.getLimit();
        String search = obtenerEntidadesRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<EntidadesJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? entidadJpaRepository.findByBorradoFalse(pageable)
                : entidadJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<Entidad> datos = pagina.getContent().stream()
                .map(EntidadJpaMapper::toDomain)
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
    public void eliminar(EntidadId entidadId) {
        EntidadesJpaModel model = entidadJpaRepository.findByIdAndBorradoFalse(aUuid(entidadId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + entidadId.getValue() + "' no existe"));

        Entidad entidad = EntidadJpaMapper.toDomain(model);
        entidad.eliminar();

        EntidadJpaMapper.copiarCambios(entidad, model);
        entidadJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
