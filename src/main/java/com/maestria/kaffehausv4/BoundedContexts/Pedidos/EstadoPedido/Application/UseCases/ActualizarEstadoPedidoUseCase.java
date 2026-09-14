package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ActualizarEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarEstadoPedidoUseCase {
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ActualizarEstadoPedidoUseCase(
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public EstadoPedidoResponse execute(String estadoPedidoId, ActualizarEstadoPedidoRequest actualizarEstadoPedidoRequest) {
        EstadoPedidoId id = new EstadoPedidoId(estadoPedidoId);

        EstadoPedido estadoPedido = estadoPedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + estadoPedidoId + "' no existe"));

        estadoPedido.actualizar(
                actualizarEstadoPedidoRequest.getNombre());

        EstadoPedido estadoPedidoActualizado = estadoPedidoRepository.actualizar(id, estadoPedido);

        return EstadoPedidoResponse.createFromAggregate(estadoPedidoActualizado);
    }
}
