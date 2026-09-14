package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Datasources.IEstadoVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class EstadoVentaDatasourceImpl implements IEstadoVentaDatasource {

    private final EstadoVentaJpaRepository estadoVentaJpaRepository;

    public EstadoVentaDatasourceImpl(EstadoVentaJpaRepository estadoVentaJpaRepository) {
        this.estadoVentaJpaRepository = estadoVentaJpaRepository;
    }

    @Override
    public EstadoVenta crear(EstadoVenta estadoVenta) {
        EstadoVentaJpaModel model = EstadoVentaJpaMapper.toPersistence(estadoVenta);
        return EstadoVentaJpaMapper.toDomain(estadoVentaJpaRepository.save(model));
    }

    @Override
    public EstadoVenta actualizar(EstadoVentaId estadoVentaId, EstadoVenta estadoVenta) {
        EstadoVentaJpaModel model = estadoVentaJpaRepository.findById(aUuid(estadoVentaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + estadoVentaId.getValue() + "' no existe"));

        EstadoVentaJpaMapper.copiarCambios(estadoVenta, model);

        return EstadoVentaJpaMapper.toDomain(estadoVentaJpaRepository.save(model));
    }

    @Override
    public Optional<EstadoVenta> obtenerPorId(EstadoVentaId estadoVentaId) {
        return estadoVentaJpaRepository.findById(aUuid(estadoVentaId.getValue()))
                .map(EstadoVentaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<EstadoVenta>> obtenerTodos(ObtenerEstadosVentaRequest obtenerEstadosVentaRequest) {
        int skip = obtenerEstadosVentaRequest.getSkip();
        int limit = obtenerEstadosVentaRequest.getLimit();
        String search = obtenerEstadosVentaRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<EstadoVentaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? estadoVentaJpaRepository.findAll(pageable)
                : estadoVentaJpaRepository.findByNombreContainingIgnoreCase(search.trim(), pageable);

        List<EstadoVenta> datos = pagina.getContent().stream()
                .map(EstadoVentaJpaMapper::toDomain)
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
    public void eliminar(EstadoVentaId estadoVentaId) {
        UUID id = aUuid(estadoVentaId.getValue());

        if (!estadoVentaJpaRepository.existsById(id)) {
            throw DomainException.throwException(404, "El estado de venta con id '" + estadoVentaId.getValue() + "' no existe");
        }

        estadoVentaJpaRepository.deleteById(id);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
