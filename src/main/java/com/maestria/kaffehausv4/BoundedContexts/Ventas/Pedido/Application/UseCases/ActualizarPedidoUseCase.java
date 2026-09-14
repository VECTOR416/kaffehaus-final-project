package com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarPedidoUseCase {
    private final IPedidoRepository pedidoRepository;
    private final IVentaRepository ventaRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public ActualizarPedidoUseCase(
            IPedidoRepository pedidoRepository,
            IVentaRepository ventaRepository,
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.ventaRepository = ventaRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public PedidoResponse execute(String pedidoId, ActualizarPedidoRequest actualizarPedidoRequest) {
        PedidoId id = new PedidoId(pedidoId);

        Pedido pedido = pedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId + "' no existe"));

        VentaId ventaId = null;
        if (actualizarPedidoRequest.getIdVenta() != null) {
            ventaId = new VentaId(actualizarPedidoRequest.getIdVenta());
            ventaRepository.obtenerPorId(ventaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + actualizarPedidoRequest.getIdVenta() + "' no existe"));
        }
        EstadoPedidoId estadoId = null;
        if (actualizarPedidoRequest.getIdEstado() != null) {
            estadoId = new EstadoPedidoId(actualizarPedidoRequest.getIdEstado());
            estadoPedidoRepository.obtenerPorId(estadoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + actualizarPedidoRequest.getIdEstado() + "' no existe"));
        }

        pedido.actualizar(
                actualizarPedidoRequest.getFechaEntrega(),
                actualizarPedidoRequest.getLatitud(),
                actualizarPedidoRequest.getLongitud(),
                actualizarPedidoRequest.getDescripcion(),
                ventaId,
                estadoId);

        Pedido pedidoActualizado = pedidoRepository.actualizar(id, pedido);

        return toResponse(pedidoActualizado);
    }

    private PedidoResponse toResponse(Pedido pedido) {
        EstadoPedido estado = estadoPedidoRepository.obtenerPorId(pedido.getEstadoId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido asociado a el pedido no existe"));
        return PedidoResponse.createFromAggregate(pedido, estado);
    }
}
