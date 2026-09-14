package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarPedidoUseCase {
    private final IPedidoRepository pedidoRepository;

    public EliminarPedidoUseCase(IPedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public void execute(String pedidoId) {
        PedidoId id = new PedidoId(pedidoId);

        pedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId + "' no existe"));

        pedidoRepository.eliminar(id);
    }
}
