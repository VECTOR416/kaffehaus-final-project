package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Dtos.CrearEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.Aggregates.EstadoPedido;

@Service
@Transactional
public class CrearEstadoPedidoUseCase {
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public CrearEstadoPedidoUseCase(
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public EstadoPedidoResponse execute(CrearEstadoPedidoRequest crearEstadoPedidoRequest) {
        EstadoPedido estadoPedido = EstadoPedido.crear(
                crearEstadoPedidoRequest.getNombre());

        EstadoPedido estadoPedidoCreado = estadoPedidoRepository.crear(estadoPedido);

        return EstadoPedidoResponse.createFromAggregate(estadoPedidoCreado);
    }
}
