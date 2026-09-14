package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Datasources;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Datasources.IMovimientoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class MovimientoDatasourceImpl implements IMovimientoDatasource {

    private final MovimientoJpaRepository movimientoJpaRepository;

    public MovimientoDatasourceImpl(MovimientoJpaRepository movimientoJpaRepository) {
        this.movimientoJpaRepository = movimientoJpaRepository;
    }

    @Override
    public Movimiento crear(Movimiento movimiento) {
        MovimientoJpaModel model = MovimientoJpaMapper.toPersistence(movimiento);
        return MovimientoJpaMapper.toDomain(movimientoJpaRepository.save(model));
    }

    @Override
    public Movimiento actualizar(MovimientoId movimientoId, Movimiento movimiento) {
        MovimientoJpaModel model = movimientoJpaRepository.findByIdAndBorradoFalse(aUuid(movimientoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El movimiento con id '" + movimientoId.getValue() + "' no existe"));

        MovimientoJpaMapper.copiarCambios(movimiento, model);

        return MovimientoJpaMapper.toDomain(movimientoJpaRepository.save(model));
    }

    @Override
    public Optional<Movimiento> obtenerPorId(MovimientoId movimientoId) {
        return movimientoJpaRepository.findByIdAndBorradoFalse(aUuid(movimientoId.getValue()))
                .map(MovimientoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Movimiento>> obtenerTodos(ObtenerMovimientosRequest obtenerMovimientosRequest) {
        int skip = obtenerMovimientosRequest.getSkip();
        int limit = obtenerMovimientosRequest.getLimit();
        String search = obtenerMovimientosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<MovimientoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? movimientoJpaRepository.findByBorradoFalse(pageable)
                : movimientoJpaRepository.findByBorradoFalseAndMotivoContainingIgnoreCase(search.trim(), pageable);

        List<Movimiento> datos = pagina.getContent().stream()
                .map(MovimientoJpaMapper::toDomain)
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
    public void eliminar(MovimientoId movimientoId) {
        MovimientoJpaModel model = movimientoJpaRepository.findByIdAndBorradoFalse(aUuid(movimientoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El movimiento con id '" + movimientoId.getValue() + "' no existe"));

        model.setBorrado(true);
        model.setFecU(OffsetDateTime.now());
        movimientoJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
