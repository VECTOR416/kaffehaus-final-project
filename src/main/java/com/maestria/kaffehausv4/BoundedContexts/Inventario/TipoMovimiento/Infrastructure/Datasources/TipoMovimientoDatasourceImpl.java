package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Datasources.ITipoMovimientoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa.TipoMovimientoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa.TipoMovimientoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa.TipoMovimientoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class TipoMovimientoDatasourceImpl implements ITipoMovimientoDatasource {

    private final TipoMovimientoJpaRepository tipoMovimientoJpaRepository;

    public TipoMovimientoDatasourceImpl(TipoMovimientoJpaRepository tipoMovimientoJpaRepository) {
        this.tipoMovimientoJpaRepository = tipoMovimientoJpaRepository;
    }

    @Override
    public TipoMovimiento crear(TipoMovimiento tipoMovimiento) {
        TipoMovimientoJpaModel model = TipoMovimientoJpaMapper.toPersistence(tipoMovimiento);
        return TipoMovimientoJpaMapper.toDomain(tipoMovimientoJpaRepository.save(model));
    }

    @Override
    public TipoMovimiento actualizar(TipoMovimientoId tipoMovimientoId, TipoMovimiento tipoMovimiento) {
        TipoMovimientoJpaModel model = tipoMovimientoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoMovimientoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + tipoMovimientoId.getValue() + "' no existe"));

        TipoMovimientoJpaMapper.copiarCambios(tipoMovimiento, model);

        return TipoMovimientoJpaMapper.toDomain(tipoMovimientoJpaRepository.save(model));
    }

    @Override
    public Optional<TipoMovimiento> obtenerPorId(TipoMovimientoId tipoMovimientoId) {
        return tipoMovimientoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoMovimientoId.getValue()))
                .map(TipoMovimientoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<TipoMovimiento>> obtenerTodos(ObtenerTiposMovimientoRequest obtenerTiposMovimientoRequest) {
        int skip = obtenerTiposMovimientoRequest.getSkip();
        int limit = obtenerTiposMovimientoRequest.getLimit();
        String search = obtenerTiposMovimientoRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<TipoMovimientoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? tipoMovimientoJpaRepository.findByBorradoFalse(pageable)
                : tipoMovimientoJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<TipoMovimiento> datos = pagina.getContent().stream()
                .map(TipoMovimientoJpaMapper::toDomain)
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
    public void eliminar(TipoMovimientoId tipoMovimientoId) {
        TipoMovimientoJpaModel model = tipoMovimientoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoMovimientoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + tipoMovimientoId.getValue() + "' no existe"));

        TipoMovimiento tipoMovimiento = TipoMovimientoJpaMapper.toDomain(model);
        tipoMovimiento.eliminar();

        TipoMovimientoJpaMapper.copiarCambios(tipoMovimiento, model);
        tipoMovimientoJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
