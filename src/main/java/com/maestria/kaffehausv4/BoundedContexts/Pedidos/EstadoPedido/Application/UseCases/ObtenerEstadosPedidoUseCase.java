package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerEstadosPedidoUseCase {
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ObtenerEstadosPedidoUseCase(
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public PaginatedResponse<List<EstadoPedidoResponse>> execute(ObtenerEstadosPedidoRequest obtenerEstadosPedidoRequest) {
        PaginatedResponse<List<EstadoPedido>> resultado = estadoPedidoRepository.obtenerTodos(obtenerEstadosPedidoRequest);

        List<EstadoPedidoResponse> estadosPedido = resultado.getData().stream()
                .map(EstadoPedidoResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                estadosPedido,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
