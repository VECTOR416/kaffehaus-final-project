package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarEstadoPedidoUseCase {
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public EliminarEstadoPedidoUseCase(IEstadoPedidoRepository estadoPedidoRepository) {
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public void execute(String estadoPedidoId) {
        EstadoPedidoId id = new EstadoPedidoId(estadoPedidoId);

        estadoPedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + estadoPedidoId + "' no existe"));

        estadoPedidoRepository.eliminar(id);
    }
}
