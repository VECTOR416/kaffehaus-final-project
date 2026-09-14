package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerEstadoPedidoPorIdUseCase {
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ObtenerEstadoPedidoPorIdUseCase(
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public EstadoPedidoResponse execute(String estadoPedidoId) {
        EstadoPedidoId id = new EstadoPedidoId(estadoPedidoId);

        EstadoPedido estadoPedido = estadoPedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + estadoPedidoId + "' no existe"));

        return EstadoPedidoResponse.createFromAggregate(estadoPedido);
    }
}
