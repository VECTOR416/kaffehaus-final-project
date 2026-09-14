package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Datasources.IEstadoPedidoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Infrastructure.Persistence.Jpa.EstadoPedidoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Infrastructure.Persistence.Jpa.EstadoPedidoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Infrastructure.Persistence.Jpa.EstadoPedidoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class EstadoPedidoDatasourceImpl implements IEstadoPedidoDatasource {

    private final EstadoPedidoJpaRepository estadoPedidoJpaRepository;

    public EstadoPedidoDatasourceImpl(EstadoPedidoJpaRepository estadoPedidoJpaRepository) {
        this.estadoPedidoJpaRepository = estadoPedidoJpaRepository;
    }

    @Override
    public EstadoPedido crear(EstadoPedido estadoPedido) {
        EstadoPedidoJpaModel model = EstadoPedidoJpaMapper.toPersistence(estadoPedido);
        return EstadoPedidoJpaMapper.toDomain(estadoPedidoJpaRepository.save(model));
    }

    @Override
    public EstadoPedido actualizar(EstadoPedidoId estadoPedidoId, EstadoPedido estadoPedido) {
        EstadoPedidoJpaModel model = estadoPedidoJpaRepository.findById(aUuid(estadoPedidoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + estadoPedidoId.getValue() + "' no existe"));

        EstadoPedidoJpaMapper.copiarCambios(estadoPedido, model);

        return EstadoPedidoJpaMapper.toDomain(estadoPedidoJpaRepository.save(model));
    }

    @Override
    public Optional<EstadoPedido> obtenerPorId(EstadoPedidoId estadoPedidoId) {
        return estadoPedidoJpaRepository.findById(aUuid(estadoPedidoId.getValue()))
                .map(EstadoPedidoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<EstadoPedido>> obtenerTodos(ObtenerEstadosPedidoRequest obtenerEstadosPedidoRequest) {
        int skip = obtenerEstadosPedidoRequest.getSkip();
        int limit = obtenerEstadosPedidoRequest.getLimit();
        String search = obtenerEstadosPedidoRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<EstadoPedidoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? estadoPedidoJpaRepository.findAll(pageable)
                : estadoPedidoJpaRepository.findByNombreContainingIgnoreCase(search.trim(), pageable);

        List<EstadoPedido> datos = pagina.getContent().stream()
                .map(EstadoPedidoJpaMapper::toDomain)
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
    public void eliminar(EstadoPedidoId estadoPedidoId) {
        UUID id = aUuid(estadoPedidoId.getValue());

        if (!estadoPedidoJpaRepository.existsById(id)) {
            throw DomainException.throwException(404, "El estado de pedido con id '" + estadoPedidoId.getValue() + "' no existe");
        }

        estadoPedidoJpaRepository.deleteById(id);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
