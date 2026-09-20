package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Datasources.ITipoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class TipoDatasourceImpl implements ITipoDatasource {

    private final TipoJpaRepository tipoJpaRepository;

    public TipoDatasourceImpl(TipoJpaRepository tipoJpaRepository) {
        this.tipoJpaRepository = tipoJpaRepository;
    }

    @Override
    public Tipo crear(Tipo tipo) {
        TipoJpaModel model = TipoJpaMapper.toPersistence(tipo);
        return TipoJpaMapper.toDomain(tipoJpaRepository.save(model));
    }

    @Override
    public Tipo actualizar(TipoId tipoId, Tipo tipo) {
        TipoJpaModel model = tipoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId.getValue() + "' no existe"));

        TipoJpaMapper.copiarCambios(tipo, model);

        return TipoJpaMapper.toDomain(tipoJpaRepository.save(model));
    }

    @Override
    public Optional<Tipo> obtenerPorId(TipoId tipoId) {
        return tipoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoId.getValue()))
                .map(TipoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Tipo>> obtenerTodos(ObtenerTiposRequest obtenerTiposRequest) {
        int skip = obtenerTiposRequest.getSkip();
        int limit = obtenerTiposRequest.getLimit();
        String search = obtenerTiposRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<TipoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? tipoJpaRepository.findByBorradoFalse(pageable)
                : tipoJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<Tipo> datos = pagina.getContent().stream()
                .map(TipoJpaMapper::toDomain)
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
    public void eliminar(TipoId tipoId) {
        TipoJpaModel model = tipoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId.getValue() + "' no existe"));

        Tipo tipo = TipoJpaMapper.toDomain(model);
        tipo.eliminar();

        TipoJpaMapper.copiarCambios(tipo, model);
        tipoJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
