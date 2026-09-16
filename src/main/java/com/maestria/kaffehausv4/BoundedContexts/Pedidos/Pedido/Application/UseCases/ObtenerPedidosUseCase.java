package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerPedidosUseCase {
    private final IPedidoRepository pedidoRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ObtenerPedidosUseCase(
            IPedidoRepository pedidoRepository,
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public PaginatedResponse<List<PedidoResponse>> execute(ObtenerPedidosRequest obtenerPedidosRequest) {
        PaginatedResponse<List<Pedido>> resultado = pedidoRepository.obtenerTodos(obtenerPedidosRequest);

        List<PedidoResponse> pedidos = resultado.getData().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                pedidos,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private PedidoResponse toResponse(Pedido pedido) {
        EstadoPedido estado = estadoPedidoRepository.obtenerPorId(pedido.getEstadoId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido asociado a el pedido no existe"));
        return PedidoResponse.createFromAggregate(pedido, estado);
    }
}
