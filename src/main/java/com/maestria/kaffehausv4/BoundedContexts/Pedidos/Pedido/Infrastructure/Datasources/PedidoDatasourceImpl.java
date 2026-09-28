package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Datasources.IPedidoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa.PedidoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa.PedidoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa.PedidoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class PedidoDatasourceImpl implements IPedidoDatasource {

    private final PedidoJpaRepository pedidoJpaRepository;

    public PedidoDatasourceImpl(PedidoJpaRepository pedidoJpaRepository) {
        this.pedidoJpaRepository = pedidoJpaRepository;
    }

    @Override
    public Pedido crear(Pedido pedido) {
        PedidoJpaModel model = PedidoJpaMapper.toPersistence(pedido);
        return PedidoJpaMapper.toDomain(pedidoJpaRepository.save(model));
    }

    @Override
    public Pedido actualizar(PedidoId pedidoId, Pedido pedido) {
        PedidoJpaModel model = pedidoJpaRepository.findByIdAndBorradoFalse(aUuid(pedidoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId.getValue() + "' no existe"));

        PedidoJpaMapper.copiarCambios(pedido, model);

        return PedidoJpaMapper.toDomain(pedidoJpaRepository.save(model));
    }

    @Override
    public Optional<Pedido> obtenerPorId(PedidoId pedidoId) {
        return pedidoJpaRepository.findByIdAndBorradoFalse(aUuid(pedidoId.getValue()))
                .map(PedidoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Pedido>> obtenerTodos(ObtenerPedidosRequest obtenerPedidosRequest) {
        int skip = obtenerPedidosRequest.getSkip();
        int limit = obtenerPedidosRequest.getLimit();
        String search = obtenerPedidosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<PedidoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? pedidoJpaRepository.findByBorradoFalse(pageable)
                : pedidoJpaRepository.findByBorradoFalseAndDescripcionContainingIgnoreCase(search.trim(), pageable);

        List<Pedido> datos = pagina.getContent().stream()
                .map(PedidoJpaMapper::toDomain)
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
    public void eliminar(PedidoId pedidoId) {
        PedidoJpaModel model = pedidoJpaRepository.findByIdAndBorradoFalse(aUuid(pedidoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId.getValue() + "' no existe"));

        Pedido pedido = PedidoJpaMapper.toDomain(model);
        pedido.eliminar();

        PedidoJpaMapper.copiarCambios(pedido, model);
        pedidoJpaRepository.save(model);
    }

    @Override
    public Optional<Pedido> obtenerPorVentaId(VentaId ventaId) {
        return pedidoJpaRepository.findFirstByIdVentaAndBorradoFalse(aUuid(ventaId.getValue()))
                .map(PedidoJpaMapper::toDomain);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
