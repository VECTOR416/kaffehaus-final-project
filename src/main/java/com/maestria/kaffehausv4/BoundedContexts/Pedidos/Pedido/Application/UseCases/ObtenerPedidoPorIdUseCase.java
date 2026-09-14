package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerPedidoPorIdUseCase {
    private final IPedidoRepository pedidoRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ObtenerPedidoPorIdUseCase(
            IPedidoRepository pedidoRepository,
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public PedidoResponse execute(String pedidoId) {
        PedidoId id = new PedidoId(pedidoId);

        Pedido pedido = pedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId + "' no existe"));

        return toResponse(pedido);
    }

    private PedidoResponse toResponse(Pedido pedido) {
        EstadoPedido estado = estadoPedidoRepository.obtenerPorId(pedido.getEstadoId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido asociado a el pedido no existe"));
        return PedidoResponse.createFromAggregate(pedido, estado);
    }
}
